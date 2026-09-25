package com.example.ocr

/**
 * Label-aware field extractor for Indian packaged-food product labels under
 * Legal Metrology (Packaged Commodities) Rules, 2011.
 *
 * Core extraction principles:
 * ───────────────────────────
 * 1. LABEL-AWARE ANCHORING: Every key field is extracted from its labelled context.
 * 2. PROXIMITY & STRUCTURE: Uses same-line colons and immediate non-label adjacent lines.
 * 3. STRICT FALSE-POSITIVE FILTERING:
 *    - Product Name rejects ingredients (e.g. "COCONUT (3%)"), nutrition values, and company names.
 *    - Brand rejects ingredients (e.g. "INVERT") and defaults to "Needs review" when uncertain.
 *    - Net Quantity prefers declared total/formula values (e.g. "84 g" from "68 g + 16 g EXTRA = 84 g")
 *      and strictly rejects nutritional table numbers.
 *    - Batch rejects "USE" from "USE BY:" and other label tokens.
 *    - License returns only clean numerical registration numbers.
 * 4. MULTI-PHOTO CANDIDATE RANKING: Evaluates photos independently and picks highest-confidence candidate.
 */
object ProductFieldExtractor {

    // -------------------------------------------------------------------------
    // Internal Result Carrier
    // -------------------------------------------------------------------------

    data class Extraction(
        val value: String,
        val confidence: Double,
        val sourcePhotoId: Int = -1,
        val sourceText: String = ""
    )

    // -------------------------------------------------------------------------
    // Label Keyword Sets (UPPERCASE)
    // -------------------------------------------------------------------------

    private val MRP_LABELS = setOf(
        "MRP", "M.R.P", "M.R.P.", "MAX RETAIL PRICE", "MAX. RETAIL PRICE",
        "MAXIMUM RETAIL PRICE", "RETAIL PRICE"
    )

    private val MFG_DATE_LABELS = setOf(
        "MFG DATE", "MFD DATE", "DATE OF MANUFACTURE", "DATE OF MFG",
        "MANUFACTURED ON", "MANUFACTURED DATE", "MANUFACTURED",
        "MFG.", "MFD.", "MFG", "MFD",
        "DATE OF PKG.", "DATE OF PKG", "DATE OF PACKING", "DATE OF PRG.", "DATE OF PRG",
        "PKG DATE", "PKG. DATE", "PRG DATE", "PRG. DATE",
        "PKD ON", "PKD.", "PKD", "PKG.", "PKG", "PACKED ON", "PACKED",
        "PRG.", "PRG",
        "MTH & YR OF MFG", "MTH&YR OF MFG"
    )

    private val EXP_DATE_LABELS = setOf(
        "EXPIRY DATE", "EXP DATE", "USE BEFORE", "BEST BEFORE",
        "USE BY DATE", "USE BY", "USE-BY", "USEBY", "VALID TILL", "EXP.", "EXP", "EXPIRY", "BB"
    )

    private val BATCH_LABELS = setOf(
        "BATCH NO.", "BATCH NUMBER", "BATCH NO", "BATCH",
        "LOT NO.", "LOT NUMBER", "LOT NO", "LOT", "B.NO", "B. NO", "B.NO."
    )

    private val MFG_BY_LABELS = setOf(
        "MANUFACTURED & MARKETED BY", "MANUFACTURED AND MARKETED BY",
        "MANUFACTURED & PACKED BY", "MANUFACTURED AND PACKED BY",
        "MANUFACTURED FOR", "MANUFACTURED BY", "MANUFACTURED",
        "MFG. BY", "MFD. BY", "MFG BY", "MFD BY",
        "PACKED & MARKETED BY", "PACKED AND MARKETED BY",
        "PACKED BY", "PACKER", "MARKETED BY", "PRODUCED BY", "MKTED BY"
    )

    private val NET_QTY_LABELS = setOf(
        "NET QUANTITY", "NET WT.", "NET WEIGHT", "NET CONTENTS", "NET CONTENT",
        "NET VOL.", "NET VOLUME", "NET QTY", "NET WT", "NET VOL",
        "PACK SIZE", "NET"
    )

    private val LICENSE_LABELS = setOf(
        "FSSAI LICENCE NUMBER", "FSSAI LICENSE NUMBER", "FSSAI LICENCE NO.",
        "FSSAI LICENSE NO.", "FSSAI LICENCE NO", "FSSAI LICENSE NO",
        "FSSAI LIC. NO.", "FSSAI LIC. NO", "FSSAI LIC NO.", "FSSAI LIC NO",
        "FSSAI", "LIC. NO.", "LIC. NO", "LIC NO.", "LIC NO",
        "LICENSE NO.", "LICENSE NO", "LICENCE NO.", "LICENCE NO",
        "REG. NO.", "REG. NO", "REG NO.", "REG NO", "REGISTRATION NO", "REGISTRATION NUMBER"
    )

    private val CONSUMER_CARE_LABELS = setOf(
        "CONSUMER CARE CELL", "CONSUMER CARE", "CUSTOMER CARE CELL", "CUSTOMER CARE",
        "HELPLINE", "CONTACT US", "TOLL FREE", "TOLLFREE"
    )

    /** Union of all recognized labels — used to avoid mistaking the next label for a value */
    private val ALL_LABEL_KEYWORDS: Set<String> =
        MRP_LABELS + MFG_DATE_LABELS + EXP_DATE_LABELS + BATCH_LABELS +
        MFG_BY_LABELS + NET_QTY_LABELS + LICENSE_LABELS + CONSUMER_CARE_LABELS +
        setOf(
            "INGREDIENTS", "INGREDIENT", "NUTRITION", "NUTRITIONAL",
            "ALLERGENS", "ALLERGEN", "CAUTION", "STORE", "KEEP", "CONTAINS",
            "VEGETARIAN", "VEGAN", "ENERGY", "PROTEIN", "CARBOHYDRATE",
            "FAT", "SODIUM", "SUGAR", "SUGARS", "ADDED SUGARS", "PER 100",
            "PER SERVE", "SERVING", "DIRECTIONS", "HOW TO USE", "STORAGE",
            "SHELF LIFE", "FOR SALE", "PHONE NO", "E-MAIL", "EMAIL",
            "UNIT SALE PRICE", "UNIT SALE", "USP", "OTHER",
            "INCLUSIVE OF ALL TAXES", "INCL. OF ALL TAXES", "INCL OF ALL TAXES", "TAXES"
        )

    // -------------------------------------------------------------------------
    // Top Known Brands Dictionary
    // -------------------------------------------------------------------------

    private val KNOWN_BRANDS = setOf(
        "PARLE", "BRITANNIA", "AMUL", "NESTLE", "ITC", "SUNFEAST", "CADBURY",
        "MONACO", "KRACKJACK", "HIDE & SEEK", "MARIE", "GOOD DAY", "TIGER",
        "HALDIRAM'S", "HALDIRAM", "BIKANO", "LAY'S", "LAYS", "KURKURE", "BINGO",
        "PEPSICO", "DABUR", "PATANJALI", "FORTUNE", "SAFFOLA", "EVEREST", "MDH",
        "CATCH", "TATA", "UNIBIC", "OREO", "BOURNVITA", "HORLICKS", "BOOST",
        "MAGGI", "KNORR", "KRAFT", "PARLE-G", "MILK BIKIS", "50-50", "NICE",
        "GS", "GOLDEN SUPER", "BROOKE BOND", "RED LABEL", "TAJ MAHAL", "WAGH BAKRI", "TETLEY"
    )

    // -------------------------------------------------------------------------
    // Rejection Filters & Guard Patterns
    // -------------------------------------------------------------------------

    /** Disallowed product name patterns explicitly prohibited from being classified as Product Name */
    private val PRODUCT_NAME_DISALLOWED_PATTERNS = listOf(
        Regex("""(?i)\bunit\s*sale(?:\s*price)?\b"""),
        Regex("""(?i)\b(?:prg\.?|date\s*of\s*prg\.?)\b"""),
        Regex("""(?i)\bm\.?r\.?p\.?\b"""),
        Regex("""(?i)\bingredients?\b"""),
        Regex("""(?i)\bnet\s*(?:weight|wt\.?|quantity|qty\.?|contents?|vol\.?)\b"""),
        Regex("""(?i)\bbatch(?:\s*no\.?)?\b"""),
        Regex("""(?i)\blot(?:\s*no\.?)?\b"""),
        Regex("""(?i)\bdate\s*of\s*p[kr]g\.?\b"""),
        Regex("""(?i)\bp[kr]d\b"""),
        Regex("""(?i)\buse\s*by\b"""),
        Regex("""(?i)\buse\s*before\b"""),
        Regex("""(?i)\bbest\s*before\b"""),
        Regex("""(?i)\b(?:customer|consumer)\s*care\b"""),
        Regex("""(?i)^\s*other\s*$"""),
        Regex("""(?i)\bother\s*(?:plastic|material)?\b"""),
        Regex("""(?i)\binclusive\s*of\s*(?:all\s*)?taxes\b"""),
        Regex("""(?i)\bincl\.?\s*of\s*(?:all\s*)?taxes\b""")
    )

    fun isDisallowedProductName(text: String): Boolean {
        val up = text.uppercase().trim()
        if (up == "OTHER" || up == "PRG" || up == "PRG." || up == "PKG" || up == "PKG." || up == "PKD" || up == "PKD.") return true
        return PRODUCT_NAME_DISALLOWED_PATTERNS.any { it.containsMatchIn(text) }
    }

    private val INGREDIENT_WORDS = setOf(
        "INVERT", "SUGAR", "SALT", "PALM", "WHEAT", "FLOUR", "MAIDA", "OIL",
        "ACIDITY", "REGULATOR", "EMULSIFIER", "LECITHIN", "SOYA", "RAISING",
        "AGENT", "AGENTS", "ADDED", "FLAVOUR", "FLAVOR", "FLAVOURS", "FLAVORS",
        "DOUGH", "COCOA", "POWDER", "VANILLA", "SYRUP", "WATER", "MILK", "SOLIDS",
        "STARCH", "PRESERVATIVE", "COLOUR", "COLOR", "STABILIZER", "DESICCATED",
        "HYDROGENATED", "EDIBLE", "VEGETABLE", "FAT", "ANTIOXIDANT", "BUTTER"
    )

    private val BATCH_REJECT = setOf(
        "USE", "BY", "BEFORE", "BEST", "DATE", "EXP", "EXPIRY", "MFG", "MFD", "PKD",
        "INDICATES", "INDICATED", "NUMBER", "NUMBERS", "FIRST", "SECOND", "CHARACTER",
        "CHARACTERS", "THE", "OF", "AND", "FOR", "WITH", "PRODUCT", "BATCH", "LOT",
        "CONTAINS", "MEANING", "MEANS", "REFER", "DETAILS", "SEE", "PLEASE", "NOTE",
        "YEAR", "FROM", "PACKED", "MANUFACTURING", "RS", "INR", "MRP", "NOT", "NO",
        "N/A", "NA", "NIL", "NONE", "NULL", "SALE", "ONLY", "INDIA", "BELOW", "ABOVE",
        "PRINTED", "SIDE", "ON", "PACKAGE", "PKG", "PRG"
    )

    private val NUTRITION_KEYWORDS = setOf(
        "ENERGY", "PROTEIN", "CARBOHYDRATE", "FAT", "SUGARS", "SUGAR", "SODIUM",
        "CHOLESTEROL", "SATURATED", "TRANS", "KCAL", "G/100G", "PER 100", "PER SERVE",
        "SERVING SIZE", "SERVING", "NUTRITIONAL", "NUTRITION", "ADDED SUGARS",
        "MUTRITIONAL", "MUTRITION", "APORN", "INFORMATION", "INFO", "APPROX",
        "RDA", "VITAMIN", "MINERAL", "DIETARY", "FIBER", "FIBRE"
    )

    fun isNutritionText(text: String): Boolean {
        val up = text.uppercase().trim()
        if (NUTRITION_KEYWORDS.any { up.contains(it) }) return true
        if (Regex("""(?i)\b[A-Z]*UTRITION[A-Z]*\b""").containsMatchIn(text)) return true
        if (Regex("""(?i)\b(?:APORN|INPORMATION|INFORMAT)\b""").containsMatchIn(text)) return true
        return false
    }

    private val FOOD_CATEGORY_WORDS = setOf(
        "BISCUITS", "BISCUIT", "COOKIES", "COOKIE", "CRACKER", "CRACKERS",
        "RUSK", "WAFER", "WAFERS", "CHIPS", "SNACK", "SNACKS", "NAMKEEN",
        "BREAD", "CAKE", "CAKES", "NOODLES", "PASTA", "ATTA", "MAIDA",
        "RICE", "DAL", "OIL", "GHEE", "SALT", "TEA", "COFFEE", "CANDY", "CHOCOLATE",
        "CTC", "DUST", "CHAI"
    )

    private val COMPANY_SUFFIX_RE = Regex("""(?i)\b(?:PVT\s*LTD|LIMITED|LTD|LLP|INC|CORP|INDUSTRIES|FOODS|PRODUCTS|ENTERPRISES|FEDERATION|BREWERIES|BEVERAGES|BAKERY)\b""")
    private val FACTORY_UNIT_PREFIX_RE = Regex("""^(?:[A-Z0-9]{1,4}\)?\s*-\s*|\([A-Z0-9]{1,4}\)\s*-?\s*|[A-Z0-9]{1,3}\)\s*)""")
    private val ADDR_FRAG_RE = Regex("""(?i)\b(?:plot|sector|phase|road|street|nagar|colony|dist|state|delhi|mumbai|block|floor|industrial|area|village|taluka|tehsil|mandal|hyderabad|bengaluru|bangalore|chennai|kolkata|pune|ahmedabad|noida|anand|gujarat|maharashtra|karnataka)\b""")
    private val PIN_RE = Regex("""\b[1-9][0-9]{5}\b""")
    private val OCR_ART_RE = Regex("""^\([A-Z0-9]{1,4}\)[^\w]""")
    private val FSSAI_14_RE = Regex("""\b(1[0-9]{13})\b""")

    private val DATE_VALUE_RE = Regex(
        """([0-3]?[0-9][./\-\s][0-1]?[0-9][./\-\s](?:20)?[2-3][0-9]""" +
        """|[0-1]?[0-9][./\-\s](?:20)?[2-3][0-9]""" +
        """|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Za-z]*[./\-\s]*(?:20)?[2-3][0-9])""",
        RegexOption.IGNORE_CASE
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun extract(
        combinedRawText: String,
        individualTexts: List<String> = listOf(combinedRawText)
    ): ProductOcrResult {

        val validPhotos = individualTexts
            .mapIndexed { i, t -> i to t }
            .filter { (_, t) -> t.isNotBlank() }

        if (validPhotos.isEmpty() && combinedRawText.isBlank()) {
            return ProductOcrResult(
                rawText = combinedRawText,
                processedPhotoCount = 0,
                totalPhotoCount = 0,
                confidenceMap = emptyConfidenceMap()
            )
        }

        val photos = validPhotos.ifEmpty { listOf(0 to combinedRawText) }
        val perPhoto = photos.map { (pid, text) -> extractFromPhoto(text, pid) }

        fun best(k: String): Extraction? = perPhoto.mapNotNull { it[k] }.maxByOrNull { it.confidence }

        val mrpEx = best("mrp")
        val mfgDateEx = best("mfgDate")
        val expDateEx = best("expDate")
        val netQtyEx = best("netQty")
        val batchEx = best("batch")
        val licEx = best("license")
        val mfgEx = best("manufacturer")
        val addrEx = best("address")
        val consEx = best("consumerCare")
        val nameEx = best("productName")
        val brandEx = best("brand")

        fun status(ex: Extraction?): String = when {
            ex == null -> "Not detected"
            ex.confidence >= 0.70 -> "Detected"
            ex.confidence >= 0.50 -> "Needs review"
            else -> "Not detected"
        }

        fun value(ex: Extraction?): String? = if (ex != null && ex.confidence >= 0.50) ex.value else null

        return ProductOcrResult(
            productName = value(nameEx),
            brand = value(brandEx),
            mrp = value(mrpEx),
            manufacturingDate = value(mfgDateEx),
            expiryDate = value(expDateEx),
            batchNumber = value(batchEx),
            netQuantity = value(netQtyEx),
            manufacturer = value(mfgEx),
            manufacturerAddress = value(addrEx),
            licenseNumber = value(licEx),
            rawText = combinedRawText,
            processedPhotoCount = photos.size,
            totalPhotoCount = individualTexts.size,
            confidenceMap = mapOf(
                "MRP" to status(mrpEx),
                "Manufacturing Date" to status(mfgDateEx),
                "Expiry Date" to status(expDateEx),
                "Net Quantity" to status(netQtyEx),
                "Batch Number" to status(batchEx),
                "Manufacturer" to status(mfgEx),
                "License Number" to status(licEx),
                "Product Name" to status(nameEx),
                "Brand" to status(brandEx),
                "Consumer Care" to status(consEx)
            )
        )
    }

    private fun emptyConfidenceMap(): Map<String, String> = mapOf(
        "MRP" to "Not detected",
        "Manufacturing Date" to "Not detected",
        "Expiry Date" to "Not detected",
        "Net Quantity" to "Not detected",
        "Batch Number" to "Not detected",
        "Manufacturer" to "Not detected",
        "License Number" to "Not detected",
        "Product Name" to "Not detected",
        "Brand" to "Not detected",
        "Consumer Care" to "Not detected"
    )

    // -------------------------------------------------------------------------
    // Per-Photo Extractor
    // -------------------------------------------------------------------------

    private fun extractFromPhoto(text: String, pid: Int): Map<String, Extraction?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val mfgExtraction = extractManufacturer(lines, pid)
        val brandExtraction = extractBrand(lines, mfgExtraction?.value, pid)

        return mapOf(
            "mrp" to extractMrp(lines, pid),
            "mfgDate" to extractMfgDate(lines, pid),
            "expDate" to extractExpDate(lines, pid),
            "netQty" to extractNetQty(lines, pid),
            "batch" to extractBatch(lines, pid),
            "license" to extractLicense(lines, text, pid),
            "manufacturer" to mfgExtraction,
            "address" to extractAddress(lines, pid),
            "consumerCare" to extractConsumerCare(lines, pid),
            "productName" to extractProductName(lines, pid),
            "brand" to brandExtraction
        )
    }

    // -------------------------------------------------------------------------
    // Helper Utilities
    // -------------------------------------------------------------------------

    private fun afterColon(line: String): String? {
        val i = line.indexOf(':')
        if (i < 0 || i >= line.length - 1) return null
        return line.substring(i + 1).trim().ifBlank { null }
    }

    private fun isLabelLine(line: String): Boolean {
        val clean = line.uppercase().trim().replace(OCR_ART_RE, "").trim()
        return ALL_LABEL_KEYWORDS.any { kw ->
            clean == kw || clean.startsWith("$kw:") ||
            clean.startsWith("$kw ") || clean.startsWith("$kw.") ||
            clean.startsWith("$kw-")
        }
    }

    private fun matchLabel(line: String, labels: Set<String>): String? {
        val clean = line.uppercase().trim().replace(OCR_ART_RE, "").trim()
        return labels.sortedByDescending { it.length }.firstOrNull { kw ->
            clean == kw || clean.startsWith("$kw:") ||
            clean.startsWith("$kw ") || clean.startsWith("$kw.") ||
            clean.startsWith("$kw-") || clean.startsWith("$kw&") ||
            clean.startsWith("$kw &")
        }
    }

    private fun valueAfterLabel(lines: List<String>, idx: Int): Pair<String, Double>? {
        val inline = afterColon(lines[idx])
        if (!inline.isNullOrBlank()) return Pair(inline, 0.90)

        val next = lines.getOrNull(idx + 1) ?: return null
        if (isLabelLine(next)) return null
        return Pair(next.trim(), 0.75)
    }

    // -------------------------------------------------------------------------
    // 1. PRODUCT NAME (Strict anti-ingredient and anti-nutrition filter)
    // -------------------------------------------------------------------------

    private fun extractProductName(lines: List<String>, pid: Int): Extraction? {
        val candidates = mutableListOf<Pair<String, Double>>()

        for (line in lines) {
            val clean = line.trim()
            if (!isEligibleProductName(clean)) continue

            var score = 0.0
            val up = clean.uppercase()
            val words = clean.split(Regex("""\s+""")).filter { it.any { c -> c.isLetter() } }

            // Prefer 2 to 5 words, or recognized single food word like "TEA"
            if (words.size in 2..5) {
                score += 3.0
            } else if (words.size == 1 && FOOD_CATEGORY_WORDS.contains(words[0].uppercase())) {
                score += 4.0
            }

            // Contains known product category noun (e.g. COOKIES, BISCUITS, RUSK, TEA, CTC, DUST)
            if (FOOD_CATEGORY_WORDS.any { up.split(Regex("""\s+""")).contains(it) || up.contains(it) }) {
                score += 5.0
            }

            // Descriptive adjectives (CRUNCHY, BUTTER, GLUCOSE, CHOCO, GOLDEN, SUPER, CTC, DUST, etc.)
            val descriptiveWords = setOf(
                "CRUNCHY", "BUTTER", "GLUCOSE", "CHOCO", "CHOCOLATE", "CREAM", "CRISPY", "PURE", "SPECIAL", "RICH",
                "GOLDEN", "SUPER", "PREMIUM", "CTC", "DUST", "STRONG", "LEAF", "GREEN", "BLACK"
            )
            if (descriptiveWords.any { up.contains(it) }) {
                score += 2.0
            }

            // Title Case or All Uppercase
            if (clean == up || clean.split(" ").all { it.firstOrNull()?.isUpperCase() == true }) {
                score += 1.0
            }

            if (score >= 4.0) {
                candidates.add(clean to (0.75 + (score / 40.0).coerceAtMost(0.20)))
            } else if (score >= 2.0) {
                candidates.add(clean to 0.60)
            }
        }

        val bestCandidate = candidates.maxByOrNull { it.second }
        return bestCandidate?.let { Extraction(it.first, it.second, pid, it.first) }
    }

    private fun isEligibleProductName(line: String): Boolean {
        val clean = line.trim()
        if (clean.length !in 3..60) return false
        if (isDisallowedProductName(clean)) return false
        if (isLabelLine(clean)) return false

        // Reject if contains parentheses or percentage (typical of ingredients e.g. "COCONUT (3%)")
        if (clean.contains("(") || clean.contains(")") || clean.contains("%")) return false
        if (clean.contains("=") || clean.contains("+")) return false

        val up = clean.uppercase()

        // Reject if contains company indicators
        if (COMPANY_SUFFIX_RE.containsMatchIn(up) || up.contains("MFD BY") || up.contains("PACKED BY")) return false

        // Reject if contains ingredient markers
        val ingredientMarkers = setOf(
            "INGREDIENT", "CONTAINS", "FLAVOUR", "EMULSIFIER", "PRESERVATIVE",
            "COLOR", "COLOUR", "STABILIZER", "DOUGH", "SYRUP", "EDIBLE VEGETABLE",
            "HYDROGENATED", "ACIDITY"
        )
        if (ingredientMarkers.any { up.contains(it) }) return false

        // Reject if contains nutrition markers
        if (NUTRITION_KEYWORDS.any { up.contains(it) }) return false

        // Reject if contains address, phone, license, sale phrases
        if (ADDR_FRAG_RE.containsMatchIn(up) || up.contains("FOR SALE") || up.contains("PHONE") || up.contains("LIC")) return false

        // Reject if contains price, dates, or quantities
        if (up.contains("MRP") || up.contains("RS.") || up.contains("RS ") || up.contains("₹") || up.contains("/-")) return false
        if (Regex("""\b[0-9]+(?:\.[0-9]+)?\s*(?:g|gm|gms|kg|ml|l)\b""", RegexOption.IGNORE_CASE).containsMatchIn(up)) return false

        // Word count check: Allow single-word if it's an established food category word (e.g. "TEA", "COFFEE")
        val words = clean.split(Regex("""\s+""")).filter { it.any { c -> c.isLetter() } }
        if (words.size == 1) {
            val single = words[0].uppercase()
            return FOOD_CATEGORY_WORDS.contains(single)
        }
        return words.size in 2..6
    }

    // -------------------------------------------------------------------------
    // 2. BRAND (Rejects ingredients like INVERT, prefers manufacturer/known brands)
    // -------------------------------------------------------------------------

    private fun extractBrand(lines: List<String>, manufacturer: String?, pid: Int): Extraction? {
        // Strategy 1: Check known brand dictionary in the lines
        for (line in lines) {
            val up = line.uppercase().trim()
            val stripped = up.replace(Regex("""[^A-Z0-9\s&']"""), "").trim()
            for (known in KNOWN_BRANDS) {
                if (up == known || stripped == known ||
                    up.startsWith("$known ") || stripped.startsWith("$known ") ||
                    up.endsWith(" $known") || stripped.endsWith(" $known") ||
                    up.contains(" $known ") || stripped.contains(" $known ")
                ) {
                    return Extraction(known, 0.95, pid, line)
                }
            }
        }

        // Strategy 2: Derive brand from confirmed manufacturer name (e.g. "PARLE BISCUITS PVT LTD" -> "PARLE")
        if (!manufacturer.isNullOrBlank()) {
            val firstWord = manufacturer.split(Regex("""[\s,]+""")).firstOrNull()?.uppercase()
            if (firstWord != null && firstWord.length >= 2 && !INGREDIENT_WORDS.contains(firstWord)) {
                return Extraction(firstWord, 0.88, pid, manufacturer)
            }
        }

        // Strategy 3: Check prominent standalone word in lines, rejecting ingredients and generic words
        for (line in lines) {
            val clean = line.trim().uppercase()
            if (clean.length in 2..25 &&
                !isLabelLine(clean) &&
                !isDisallowedProductName(clean) &&
                !INGREDIENT_WORDS.contains(clean) &&
                !FOOD_CATEGORY_WORDS.contains(clean) &&
                !NUTRITION_KEYWORDS.contains(clean) &&
                clean.all { it.isLetter() || it == ' ' || it == '&' || it == '\'' }
            ) {
                val words = clean.split(" ")
                if (words.size in 1..2 && KNOWN_BRANDS.contains(words[0])) {
                    return Extraction(clean, 0.90, pid, line)
                }
            }
        }

        // If confidence is low or uncertain, return null (displayed as "Needs review")
        return null
    }

    // -------------------------------------------------------------------------
    // 3. MRP (Handles "MRP 10.00", "MRP: ₹10.00", avoids nutritional numbers)
    // -------------------------------------------------------------------------

    private val MRP_INLINE_CLEAN_RE = Regex(
        """(?i)\b(?:M\.?\s*R\.?\s*P\.?|MAX(?:IMUM)?\s+RETAIL\s+PRICE|RETAIL\s+PRICE)\s*[:\-.]*\s*(?:RS\.?|INR|₹)?\s*([0-9]+(?:\.[0-9]{1,2})?)(?:\s*/-)?"""
    )

    private fun extractMrp(lines: List<String>, pid: Int): Extraction? {
        val candidates = mutableListOf<Extraction>()

        for ((idx, line) in lines.withIndex()) {
            val up = line.uppercase()
            // Reject lines belonging to nutrition tables (e.g. Added Sugars 25.9)
            if (isNutritionText(line) && !up.contains("MRP") && !up.contains("RETAIL PRICE")) continue

            // Reject lines that are purely Unit Sale Price declarations without MRP
            if ((up.contains("UNIT SALE PRICE") || up.contains("UNIT SALE")) && !up.contains("MRP") && !up.contains("RETAIL PRICE") && !up.contains("MAX")) continue

            // 1. Inline MRP match
            val m = MRP_INLINE_CLEAN_RE.find(line)
            if (m != null) {
                var amt = m.groupValues[1].trim()

                // Check if a single digit was cut off before 0.00 or .00 (e.g. "MRP 1" and next token "0.00" or line "0.00")
                if (amt.length == 1) {
                    val afterMatch = line.substring(m.range.last + 1).trim()
                    val nextToken = afterMatch.split(Regex("""\s+""")).firstOrNull() ?: ""
                    if (nextToken.matches(Regex("""0(?:\.00)?""")) || nextToken.matches(Regex("""\.[0-9]{2}"""))) {
                        amt += nextToken
                    } else {
                        val nextLine = lines.getOrNull(idx + 1)?.trim() ?: ""
                        if (nextLine.matches(Regex("""0(?:\.00)?""")) || nextLine.matches(Regex("""\.[0-9]{2}"""))) {
                            amt += nextLine
                        }
                    }
                }

                val d = amt.toDoubleOrNull()
                if (d != null && d in 1.0..50000.0) {
                    val conf = scoreMrp(amt, d)
                    val formatted = normalizeMrpString(amt)
                    candidates.add(Extraction(formatted, conf, pid, line))
                }
            }

            // 2. Label on this line, amount on next line
            if (matchLabel(line, MRP_LABELS) != null) {
                val next = lines.getOrNull(idx + 1) ?: continue
                if (!isLabelLine(next) && !isNutritionText(next)) {
                    val am = Regex("""(?:RS\.?|INR|₹)?\s*([0-9]+(?:\.[0-9]{1,2})?)""").find(next)
                    if (am != null) {
                        val amt = am.groupValues[1].trim()
                        val d = amt.toDoubleOrNull()
                        if (d != null && d in 1.0..50000.0) {
                            val conf = scoreMrp(amt, d) - 0.05
                            candidates.add(Extraction(normalizeMrpString(amt), conf, pid, "$line -> $next"))
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        // Pick highest-scoring candidate (full decimal format e.g. ₹10.00 will strongly beat truncated digits)
        val best = candidates.maxByOrNull { it.confidence } ?: return null
        return if (best.confidence >= 0.50) best else null
    }

    private fun scoreMrp(amt: String, d: Double): Double {
        // Severe penalty for single digit prices < 5 with no decimals (e.g. 1, 2, 3)
        // because they are almost always OCR truncations or artifacts
        if (d < 5.0 && !amt.contains(".")) {
            return 0.35 // low confidence, will NOT qualify as Detected (needs >= 0.70)
        }
        var score = 0.88
        if (amt.contains(".")) score += 0.08 // decimal e.g. 10.00 is high-confidence
        if (d >= 5.0) score += 0.02
        return score.coerceAtMost(0.98)
    }

    private fun normalizeMrpString(amt: String): String {
        val clean = amt.trim()
        return if (clean.contains(".")) {
            val parts = clean.split(".")
            if (parts.size == 2 && parts[1].length == 1) {
                "₹${parts[0]}.${parts[1]}0"
            } else {
                "₹$clean"
            }
        } else {
            "₹$clean"
        }
    }

    // -------------------------------------------------------------------------
    // 4. NET QUANTITY (Handles "68 g + 16 g EXTRA = 84 g", avoids nutrition)
    // -------------------------------------------------------------------------

    private val NET_QTY_FORMULA_RE = Regex(
        """(?i)=\s*([0-9]+(?:\.[0-9]+)?\s*(?:g(?:m|ms)?|kg|ml|l(?:tr?s?|it(?:er|re)s?)?|pieces|pcs))\b"""
    )
    private val NET_QTY_INLINE_RE = Regex(
        """(?i)\b(?:NET\s*(?:QUANTITY|WT\.?|WEIGHT|CONTENTS?|CONTENT|VOL\.?|VOLUME|QTY\.?)|PACK\s*SIZE)\s*[:\-.]*\s*([0-9]+(?:\.[0-9]+)?\s*(?:g(?:m|ms)?|kg|ml|l(?:tr?s?|it(?:er|re)s?)?|pieces|pcs))\b"""
    )
    private val STANDALONE_QTY_RE = Regex(
        """\b([0-9]+(?:\.[0-9]+)?\s*(?:g(?:m|ms)?|kg|ml|l(?:tr?s?|it(?:er|re)s?)?|pieces|pcs))\b""",
        RegexOption.IGNORE_CASE
    )

    private fun extractNetQty(lines: List<String>, pid: Int): Extraction? {
        // Priority 1: Declared total from formula: "68 g +16 g EXTRA = 84 g"
        for (line in lines) {
            val up = line.uppercase()
            if (NUTRITION_KEYWORDS.any { up.contains(it) }) continue

            val formulaMatch = NET_QTY_FORMULA_RE.find(line)
            if (formulaMatch != null) {
                val totalQty = normalizeQty(formulaMatch.groupValues[1])
                return Extraction(totalQty, 0.95, pid, line)
            }
        }

        // Priority 2: Inline labelled Net Quantity (e.g. "NET WEIGHT 100g", "NET WT: 100 gm")
        for (line in lines) {
            val up = line.uppercase()
            if (NUTRITION_KEYWORDS.any { up.contains(it) }) continue

            val inlineMatch = NET_QTY_INLINE_RE.find(line)
            if (inlineMatch != null) {
                val qty = normalizeQty(inlineMatch.groupValues[1])
                return Extraction(qty, 0.95, pid, line)
            }
        }

        // Priority 3: Standard labelled Net Quantity
        for ((idx, line) in lines.withIndex()) {
            val up = line.uppercase()
            if (NUTRITION_KEYWORDS.any { up.contains(it) }) continue

            if (matchLabel(line, NET_QTY_LABELS) != null) {
                // Same line check
                val sqm = STANDALONE_QTY_RE.find(line)
                if (sqm != null) {
                    return Extraction(normalizeQty(sqm.value), 0.92, pid, line)
                }

                // Check line immediately before (e.g. "68 g + 16 g EXTRA = 84 g" followed by "NET WEIGHT")
                val prev = lines.getOrNull(idx - 1)
                if (prev != null && !isLabelLine(prev)) {
                    val prevFormula = NET_QTY_FORMULA_RE.find(prev)
                    if (prevFormula != null) {
                        return Extraction(normalizeQty(prevFormula.groupValues[1]), 0.95, pid, prev)
                    }
                    val pqm = STANDALONE_QTY_RE.find(prev)
                    if (pqm != null) {
                        return Extraction(normalizeQty(pqm.value), 0.88, pid, prev)
                    }
                }

                // Check next line
                val (raw, conf) = valueAfterLabel(lines, idx) ?: continue
                val nqm = STANDALONE_QTY_RE.find(raw) ?: continue
                return Extraction(normalizeQty(nqm.value), conf, pid, "$line -> $raw")
            }
        }
        return null
    }

    private fun normalizeQty(raw: String): String {
        var clean = raw.trim()
        clean = clean.replace(Regex("""(?i)(?<=\d)\s*(?=[a-zA-Z])"""), " ")
        clean = clean.replace(Regex("""(?i)(?<![a-zA-Z])g(?:m|ms|r|rs)?\b"""), "g")
        clean = clean.replace(Regex("""(?i)(?<![a-zA-Z])kgs?\b"""), "kg")
        clean = clean.replace(Regex("""(?i)(?<![a-zA-Z])(?:ml|mL)\b"""), "ml")
        clean = clean.replace(Regex("""(?i)(?<![a-zA-Z])(?:ltr?s?|lit(?:er|re)s?|l)\b"""), "L")
        clean = clean.replace(Regex("""(?i)(?<![a-zA-Z])pcs\b"""), "pcs")
        clean = clean.replace(Regex("""(?i)(?<![a-zA-Z])pieces\b"""), "pieces")
        return clean.replace(Regex("""\s+"""), " ").trim()
    }

    // -------------------------------------------------------------------------
    // 5. MANUFACTURER (Prefers contextual declarations like "PARLE BISCUITS PVT LTD")
    // -------------------------------------------------------------------------

    private fun extractManufacturer(lines: List<String>, pid: Int): Extraction? {
        val candidates = mutableListOf<Extraction>()

        // Priority 1: Labeled manufacturer lines with contextual triggers
        for ((idx, line) in lines.withIndex()) {
            val kw = matchLabel(line, MFG_BY_LABELS) ?: continue

            // 1a. Inline text after colon or label
            val inlineVal = if (line.contains(':')) {
                afterColon(line)
            } else {
                val clean = line.replace(OCR_ART_RE, "").trim()
                val rem = clean.substring(kw.length).trim().removePrefix(":").removePrefix(".").trim()
                rem.ifBlank { null }
            }
            if (!inlineVal.isNullOrBlank()) {
                val company = cleanCompanyName(inlineVal)
                if (company != null) {
                    val score = scoreManufacturerCandidate(company, hasMfdTrigger = true)
                    if (score > 0.0) {
                        candidates.add(Extraction(company, score, pid, "$line -> $inlineVal"))
                    }
                }
            }

            // 1b. Contextual lookahead: lines 1 to 4 following MFD BY (factory codes or company lines)
            for (j in 1..4) {
                val nxt = lines.getOrNull(idx + j) ?: break
                if (isNutritionText(nxt)) continue
                // If it hits an unrelated major section label (MRP, BATCH, NET QTY), stop
                if (matchLabel(nxt, MRP_LABELS) != null || matchLabel(nxt, BATCH_LABELS) != null || matchLabel(nxt, NET_QTY_LABELS) != null) {
                    break
                }
                val company = cleanCompanyName(nxt)
                if (company != null) {
                    val score = scoreManufacturerCandidate(company, hasMfdTrigger = true)
                    if (score > 0.0) {
                        candidates.add(Extraction(company, score, pid, "$line -> $nxt"))
                    }
                }
            }
        }

        // Priority 2: Consumer care line specifying brand owner/manufacturer
        for (line in lines) {
            if (isNutritionText(line)) continue
            if (line.contains("CONSUMER CARE CELL:", ignoreCase = true) || line.contains("CUSTOMER CARE CELL:", ignoreCase = true)) {
                val rem = line.substringAfter(":", "").trim()
                if (rem.isNotBlank()) {
                    val company = cleanCompanyName(rem)
                    if (company != null) {
                        val score = (scoreManufacturerCandidate(company, hasMfdTrigger = false) + 0.05).coerceAtMost(0.95)
                        if (score > 0.0) {
                            candidates.add(Extraction(company, score, pid, line))
                        }
                    }
                }
            }
        }

        // Priority 3: Standalone company lines with corporate suffix (rejecting nutrition and addresses)
        for (line in lines) {
            if (isLabelLine(line) || isNutritionText(line) || ADDR_FRAG_RE.containsMatchIn(line)) continue
            if (COMPANY_SUFFIX_RE.containsMatchIn(line)) {
                val company = cleanCompanyName(line)
                if (company != null) {
                    val score = scoreManufacturerCandidate(company, hasMfdTrigger = false)
                    if (score > 0.0) {
                        candidates.add(Extraction(company, score, pid, line))
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        // Pick highest-confidence contextual candidate
        val best = candidates.maxByOrNull { it.confidence } ?: return null
        return if (best.confidence >= 0.50) best else null
    }

    private fun scoreManufacturerCandidate(name: String, hasMfdTrigger: Boolean): Double {
        if (isNutritionText(name)) return 0.0
        val up = name.uppercase()

        var score = 0.60
        if (hasMfdTrigger) score += 0.22

        if (COMPANY_SUFFIX_RE.containsMatchIn(up)) score += 0.12
        if (up.contains("FOODS") || up.contains("PRODUCTS") || up.contains("BISCUITS") || up.contains("BAKERY") || up.contains("INDUSTRIES")) score += 0.05
        if (KNOWN_BRANDS.any { up.startsWith("$it ") || up.contains(" $it ") }) score += 0.04

        if (name.length < 8) score -= 0.15
        return score.coerceIn(0.0, 0.98)
    }

    private fun cleanCompanyName(raw: String): String? {
        if (isNutritionText(raw)) return null

        var clean = raw.split(",").firstOrNull()?.trim() ?: return null
        clean = clean.replace(FACTORY_UNIT_PREFIX_RE, "").trim()
        clean = clean.replace(OCR_ART_RE, "").trim()
        clean = clean.removePrefix("-").removePrefix(":").removePrefix(".").trim()

        if (clean.length < 4) return null
        if (isNutritionText(clean)) return null
        if (isLabelLine(clean) && !clean.contains("PVT") && !clean.contains("LTD") && !clean.contains("FOODS")) return null
        if (ADDR_FRAG_RE.containsMatchIn(clean) && !COMPANY_SUFFIX_RE.containsMatchIn(clean)) return null

        val up = clean.uppercase()
        val hasCompanyIndicator = COMPANY_SUFFIX_RE.containsMatchIn(up) ||
                up.contains("FOODS") || up.contains("PRODUCTS") || up.contains("INDUSTRIES") ||
                KNOWN_BRANDS.any { up.startsWith(it) }

        val words = clean.split(Regex("""\s+""")).filter { it.any { c -> c.isLetter() } }
        if (!hasCompanyIndicator && words.size < 2) return null

        return clean
    }

    // -------------------------------------------------------------------------
    // 6. MANUFACTURER ADDRESS (Must look like genuine postal address)
    // -------------------------------------------------------------------------

    private fun extractAddress(lines: List<String>, pid: Int): Extraction? {
        for ((idx, line) in lines.withIndex()) {
            if (matchLabel(line, MFG_BY_LABELS) == null) continue
            val (raw, _) = valueAfterLabel(lines, idx) ?: continue

            val parts = raw.split(",", limit = 2)
            val addrLines = mutableListOf<String>()
            parts.getOrNull(1)?.trim()?.let { if (it.isNotBlank()) addrLines.add(it) }

            val valLine = if (afterColon(line) != null) idx else idx + 1
            for (j in (valLine + 1)..(valLine + 3)) {
                val nxt = lines.getOrNull(j) ?: break
                if (isLabelLine(nxt)) break
                if (nxt.isNotBlank()) addrLines.add(nxt.trim())
            }

            val fullAddr = addrLines.joinToString(", ").trim()
            if (isRealPostalAddress(fullAddr)) {
                return Extraction(fullAddr, 0.75, pid, line)
            }
        }
        return null
    }

    private fun isRealPostalAddress(text: String): Boolean {
        if (text.length < 12) return false
        var score = 0
        if (PIN_RE.containsMatchIn(text)) score += 2
        if (ADDR_FRAG_RE.containsMatchIn(text)) score += 1
        if (text.contains(",")) score += 1
        return score >= 2
    }

    // -------------------------------------------------------------------------
    // 7. LICENSE / REGISTRATION NUMBER (Only clean digits)
    // -------------------------------------------------------------------------

    private val LIC_NUMBER_RE = Regex(
        """(?i)\b(?:LIC\.?\s*NO\.?|LICENSE\s*NO\.?|LICENCE\s*NO\.?|REG(?:ISTRATION)?\s*NO\.?|FSSAI(?:\s*LIC\.?\s*NO\.?)?)\s*[:\-.]*\s*([0-9]{10,14})\b"""
    )

    private fun extractLicense(lines: List<String>, rawText: String, pid: Int): Extraction? {
        // Priority 1: Labelled license number e.g. "LIC. No.: 10012051000117" or "LIC NO. 10015042000123"
        for (line in lines) {
            val m = LIC_NUMBER_RE.find(line)
            if (m != null) {
                return Extraction(m.groupValues[1].trim(), 0.95, pid, line)
            }
        }

        // Priority 2: Standalone 14-digit FSSAI number
        val fssai = FSSAI_14_RE.find(rawText)
        if (fssai != null) {
            return Extraction(fssai.groupValues[1], 0.92, pid, fssai.value)
        }

        // Priority 3: Label followed by value on same/next line
        for ((idx, line) in lines.withIndex()) {
            matchLabel(line, LICENSE_LABELS) ?: continue
            val (raw, conf) = valueAfterLabel(lines, idx) ?: continue
            val digits = raw.filter { it.isDigit() }
            if (digits.length in 10..14) {
                return Extraction(digits, conf, pid, "$line -> $raw")
            }
        }

        return null
    }

    // -------------------------------------------------------------------------
    // 8. BATCH NUMBER (Never extracts "USE" from "USE BY")
    // -------------------------------------------------------------------------

    private val BATCH_INLINE_RE = Regex(
        """(?i)\b(?:BATCH\s*NO\.?|BATCH\s*NUMBER|BATCH|LOT\s*NO\.?|LOT\s*NUMBER|LOT|B\.?\s*NO\.?)\s*[:\-.]*\s*([A-Z0-9\-_/]{2,24})\b"""
    )

    private fun extractBatch(lines: List<String>, pid: Int): Extraction? {
        // Priority 1: Inline match (e.g. "BATCH NO B-42" or "BATCH NO: B-42")
        for (line in lines) {
            val m = BATCH_INLINE_RE.find(line)
            if (m != null) {
                val token = m.groupValues[1].trim()
                if (isBatchValid(token)) {
                    return Extraction(token, 0.95, pid, line)
                }
            }
        }

        // Priority 2: Label followed by remainder or next line
        for ((idx, line) in lines.withIndex()) {
            val kw = matchLabel(line, BATCH_LABELS) ?: continue

            // 1. Check same line after colon or label
            val sameLineRemainder = if (line.contains(':')) {
                afterColon(line)
            } else {
                val clean = line.replace(OCR_ART_RE, "").trim()
                val rem = clean.substring(kw.length).trim().removePrefix(":").removePrefix(".").trim()
                rem.ifBlank { null }
            }

            if (!sameLineRemainder.isNullOrBlank()) {
                val token = sameLineRemainder.split(Regex("""[\s,;]+""")).firstOrNull()?.trim()
                if (token != null && isBatchValid(token)) {
                    return Extraction(token, 0.92, pid, line)
                }
            }

            // 2. Check next line ONLY if it is not another label keyword, nutrition line, or date label
            val next = lines.getOrNull(idx + 1) ?: continue
            if (isLabelLine(next) || isNutritionText(next)) continue

            val token = next.split(Regex("""[\s,;]+""")).firstOrNull()?.trim() ?: continue
            if (isBatchValid(token)) {
                return Extraction(token, 0.85, pid, "$line -> $next")
            }
        }
        return null
    }

    private fun isBatchValid(v: String): Boolean {
        val up = v.uppercase().trim()
        if (BATCH_REJECT.contains(up)) return false
        if (up.startsWith("NOT") || up.startsWith("USE") || up.startsWith("PKD") || up.startsWith("DATE") || up.startsWith("FOR")) return false
        if (ALL_LABEL_KEYWORDS.any { up == it || up.startsWith("$it:") || up.startsWith("$it ") }) return false
        if (v.length !in 2..24) return false
        if (v.startsWith("Rs") || v.startsWith("INR") || v.startsWith("₹")) return false
        if (v.matches(Regex("""20[2-9][0-9]"""))) return false
        if (v.matches(Regex("""[0-9]{4}"""))) return false

        // Plain English words consisting exclusively of letters (without any digits)
        // are rejected (e.g. "Not", "Use", "Sale", "Only", "Parle", "Cookies").
        // Real batch/lot codes are alphanumeric or contain digits (e.g. ABC123, B4092X, GD-8821, LOT-2026, B-42).
        if (v.all { it.isLetter() }) return false

        return true
    }

    // -------------------------------------------------------------------------
    // 9. MANUFACTURING DATE & 10. EXPIRY DATE
    // -------------------------------------------------------------------------

    private val MFG_DATE_INLINE_RE = Regex(
        """(?i)\b(?:DATE\s*OF\s*P[KR]G\.?|DATE\s*OF\s*PACKING|DATE\s*OF\s*MFG\.?|DATE\s*OF\s*MANUFACTURE|MFG\s*DATE|MFD\s*DATE|MANUFACTURED\s*ON|MANUFACTURED\s*DATE|MANUFACTURED|MFG\.?|MFD\.?|P[KR]D\s*ON|P[KR]D\.?|P[KR]G\.?|PACKED\s*ON|PACKED|P[KR]G\s*DATE|MTH\s*&\s*YR\s*OF\s*MFG)\s*[:\-.]*\s*([0-3]?[0-9][./\-\s][0-1]?[0-9][./\-\s](?:20)?[2-3][0-9]|[0-1]?[0-9][./\-\s](?:20)?[2-3][0-9]|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Za-z]*[./\-\s]*(?:20)?[2-3][0-9])\b"""
    )

    private val EXP_DATE_INLINE_RE = Regex(
        """(?i)\b(?:EXPIRY\s*DATE|EXP\s*DATE|USE\s*BEFORE|BEST\s*BEFORE|USE\s*BY\s*DATE|USE\s*BY|USE-BY|USEBY|VALID\s*TILL|EXP\.?|EXPIRY|BB)\s*[:\-.]*\s*([0-3]?[0-9][./\-\s][0-1]?[0-9][./\-\s](?:20)?[2-3][0-9]|[0-1]?[0-9][./\-\s](?:20)?[2-3][0-9]|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Za-z]*[./\-\s]*(?:20)?[2-3][0-9])\b"""
    )

    private fun extractMfgDate(lines: List<String>, pid: Int): Extraction? {
        // Priority 1: Direct inline match (e.g. "DATE OF PKG. 05/2026", "PKD 05/2026")
        for (line in lines) {
            val m = MFG_DATE_INLINE_RE.find(line)
            if (m != null) {
                return Extraction(normalizeDate(m.groupValues[1]), 0.95, pid, line)
            }
        }

        // Priority 2: Label followed by value on same/next line
        for ((idx, line) in lines.withIndex()) {
            val kw = matchLabel(line, MFG_DATE_LABELS) ?: continue

            val sameLineRemainder = if (line.contains(':')) {
                afterColon(line)
            } else {
                val clean = line.replace(OCR_ART_RE, "").trim()
                clean.substring(kw.length).trim().removePrefix(":").removePrefix(".").trim().ifBlank { null }
            }

            if (!sameLineRemainder.isNullOrBlank()) {
                val dm = DATE_VALUE_RE.find(sameLineRemainder)
                if (dm != null) {
                    return Extraction(normalizeDate(dm.value), 0.92, pid, line)
                }
            }

            val (raw, conf) = valueAfterLabel(lines, idx) ?: continue
            val dm = DATE_VALUE_RE.find(raw) ?: continue
            return Extraction(normalizeDate(dm.value), conf, pid, "$line -> $raw")
        }
        return null
    }

    private fun extractExpDate(lines: List<String>, pid: Int): Extraction? {
        // Priority 1: Direct inline match (e.g. "USE BY 11/2026", "EXP 15/07/2026")
        for (line in lines) {
            val m = EXP_DATE_INLINE_RE.find(line)
            if (m != null) {
                return Extraction(normalizeDate(m.groupValues[1]), 0.95, pid, line)
            }
        }

        // Priority 2: Label followed by value on same/next line
        for ((idx, line) in lines.withIndex()) {
            val kw = matchLabel(line, EXP_DATE_LABELS) ?: continue

            val sameLineRemainder = if (line.contains(':')) {
                afterColon(line)
            } else {
                val clean = line.replace(OCR_ART_RE, "").trim()
                clean.substring(kw.length).trim().removePrefix(":").removePrefix(".").trim().ifBlank { null }
            }

            if (!sameLineRemainder.isNullOrBlank()) {
                val dm = DATE_VALUE_RE.find(sameLineRemainder)
                if (dm != null) {
                    return Extraction(normalizeDate(dm.value), 0.92, pid, line)
                }
            }

            val (raw, conf) = valueAfterLabel(lines, idx) ?: continue
            val dm = DATE_VALUE_RE.find(raw) ?: continue
            return Extraction(normalizeDate(dm.value), conf, pid, "$line -> $raw")
        }
        return null
    }

    private fun normalizeDate(raw: String): String =
        raw.trim().replace(Regex("""\s+"""), "/").replace("-", "/").replace(".", "/")

    // -------------------------------------------------------------------------
    // 11. CONSUMER CARE (Captures consumer care cell, phone, email)
    // -------------------------------------------------------------------------

    private fun extractConsumerCare(lines: List<String>, pid: Int): Extraction? {
        val details = mutableListOf<String>()

        for ((idx, line) in lines.withIndex()) {
            val up = line.uppercase()
            if (matchLabel(line, CONSUMER_CARE_LABELS) != null ||
                up.contains("CONSUMER CARE") ||
                up.contains("CUSTOMER CARE") ||
                up.contains("PHONE NO") ||
                up.contains("E-MAIL") ||
                up.contains("HELPLINE")
            ) {
                val text = line.trim()
                if (!details.contains(text)) {
                    details.add(text)
                }
                // Check following 1-2 lines for phone/email
                for (j in 1..2) {
                    val next = lines.getOrNull(idx + j) ?: break
                    val nup = next.uppercase()
                    if (nup.contains("PHONE") || nup.contains("E-MAIL") || nup.contains("EMAIL") || nup.contains("1800")) {
                        if (!details.contains(next.trim())) details.add(next.trim())
                    }
                }
            }
        }

        return if (details.isNotEmpty()) {
            Extraction(details.joinToString("; "), 0.90, pid, details.first())
        } else {
            null
        }
    }
}
