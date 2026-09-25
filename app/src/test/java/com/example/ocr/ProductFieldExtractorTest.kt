package com.example.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFieldExtractorTest {

    @Test
    fun `extracts all fields from standard product packaging label`() {
        val ocrText = """
            BRITANNIA
            Good Day Butter Cookies
            NET QTY: 100 g
            MRP ₹50.00 (INCL. OF ALL TAXES)
            MFG 15/01/2026
            EXP 15/07/2026
            BATCH NO: GD-8821
            Manufactured by Britannia Industries Ltd, Plot 14, Whitefield Road, Bengaluru - 560066
            FSSAI Lic. No. 10015042000123
        """.trimIndent()

        val result = ProductFieldExtractor.extract(ocrText)

        assertEquals("₹50.00", result.mrp)
        assertEquals("15/01/2026", result.manufacturingDate)
        assertEquals("15/07/2026", result.expiryDate)
        assertEquals("100 g", result.netQuantity)
        assertEquals("GD-8821", result.batchNumber)
        assertNotNull(result.manufacturer)
        assertTrue(result.manufacturer!!.contains("Britannia Industries"))
        assertEquals("10015042000123", result.licenseNumber)
        assertEquals("Detected", result.confidenceMap["MRP"])
        assertEquals("Detected", result.confidenceMap["Manufacturing Date"])
    }

    @Test
    fun `extracts MRP variations correctly`() {
        // Case A: MRP Rs. 120
        val textA = "Max Retail Price: Rs. 120/- incl of all taxes"
        val resA = ProductFieldExtractor.extract(textA)
        assertEquals("₹120", resA.mrp)

        // Case B: M.R.P. ₹249.50
        val textB = "M.R.P. ₹249.50"
        val resB = ProductFieldExtractor.extract(textB)
        assertEquals("₹249.50", resB.mrp)

        // Case C: MRP ₹15
        val textC = "MRP ₹15"
        val resC = ProductFieldExtractor.extract(textC)
        assertEquals("₹15", resC.mrp)
    }

    @Test
    fun `extracts manufacturing and expiry dates accurately without confusing random numbers`() {
        val ocrText = """
            Customer Care: 1800-200-9999
            Barcode: 8901234567890
            MFD: 09/2025
            USE BEFORE: 09/2027
            Call between 9 AM to 5 PM
        """.trimIndent()

        val result = ProductFieldExtractor.extract(ocrText)

        assertEquals("09/2025", result.manufacturingDate)
        assertEquals("09/2027", result.expiryDate)
    }

    @Test
    fun `extracts various net quantity formats`() {
        val text1 = "Net Content: 500 ml"
        assertEquals("500 ml", ProductFieldExtractor.extract(text1).netQuantity)

        val text2 = "NET WEIGHT 1 kg"
        assertEquals("1 kg", ProductFieldExtractor.extract(text2).netQuantity)

        val text3 = "Pack Size: 10 pieces"
        assertEquals("10 pieces", ProductFieldExtractor.extract(text3).netQuantity)

        val text4 = "Net Wt. 250g"
        assertEquals("250 g", ProductFieldExtractor.extract(text4).netQuantity)
    }

    @Test
    fun `extracts batch number with lot variations`() {
        val text1 = "LOT NO. B4092X Packed in hygiene facility"
        assertEquals("B4092X", ProductFieldExtractor.extract(text1).batchNumber)

        val text2 = "BATCH: LOT-2026-IND"
        assertEquals("LOT-2026-IND", ProductFieldExtractor.extract(text2).batchNumber)
    }

    @Test
    fun `extracts manufacturer and address`() {
        val text = """
            Manufactured & Marketed by: Himalayan Apiaries Pvt Ltd, Sector 62, Industrial Area, Noida, India - 201301
        """.trimIndent()

        val result = ProductFieldExtractor.extract(text)
        assertNotNull(result.manufacturer)
        assertTrue(result.manufacturer!!.contains("Himalayan Apiaries"))
    }

    @Test
    fun `handles empty and unreadable text gracefully with no fake values`() {
        val emptyText = "   "
        val result = ProductFieldExtractor.extract(emptyText)

        assertNull(result.mrp)
        assertNull(result.manufacturingDate)
        assertNull(result.expiryDate)
        assertNull(result.batchNumber)
        assertNull(result.netQuantity)
        assertNull(result.manufacturer)
        assertNull(result.licenseNumber)
        assertEquals("Not detected", result.confidenceMap["MRP"])
        assertEquals(0, result.processedPhotoCount)
    }

    @Test
    fun `combines multi-photo OCR results accurately`() {
        // Photo 1: Front label
        val photo1Text = "Amul Pure Ghee\nNet Qty: 1 L"
        // Photo 2: Price & Batch label
        val photo2Text = "MRP ₹650\nMFG: 02/2026\nEXP: 02/2027\nBatch No: GHEE-99"
        // Photo 3: Manufacturer info
        val photo3Text = "Manufactured by Gujarat Cooperative Milk Marketing Federation Ltd, Anand - 388001\nFSSAI 10014021000001"

        val combined = "$photo1Text\n\n---\n\n$photo2Text\n\n---\n\n$photo3Text"
        val result = ProductFieldExtractor.extract(combined, listOf(photo1Text, photo2Text, photo3Text))

        assertEquals("₹650", result.mrp)
        assertEquals("02/2026", result.manufacturingDate)
        assertEquals("02/2027", result.expiryDate)
        assertEquals("1 L", result.netQuantity)
        assertEquals("GHEE-99", result.batchNumber)
        assertNotNull(result.manufacturer)
        assertTrue(result.manufacturer!!.contains("Gujarat Cooperative"))
        assertEquals("10014021000001", result.licenseNumber)
        assertEquals(3, result.processedPhotoCount)
        assertEquals(3, result.totalPhotoCount)
    }

    @Test
    fun `ProductOcrResult model serialization and editing`() {
        val original = ProductOcrResult(
            productName = "Sample Cookies",
            brand = "Sample Brand",
            mrp = "₹50",
            manufacturingDate = "01/2026",
            expiryDate = "01/2027",
            batchNumber = "B100",
            netQuantity = "100 g",
            manufacturer = "Sample Foods Ltd",
            processedPhotoCount = 2,
            totalPhotoCount = 2,
            confidenceMap = mapOf("MRP" to "Detected")
        )

        val map = original.toMap()
        assertEquals("₹50", map["mrp"])
        assertEquals("Sample Cookies", map["productName"])

        val reconstructed = ProductOcrResult.fromMap(map)
        assertEquals(original.productName, reconstructed.productName)
        assertEquals(original.mrp, reconstructed.mrp)
        assertEquals(original.batchNumber, reconstructed.batchNumber)
        assertEquals(original.processedPhotoCount, reconstructed.processedPhotoCount)

        // Test editing in-place
        val edited = reconstructed.copy(mrp = "₹55")
        assertEquals("₹55", edited.mrp)
    }

    @Test
    fun `real biscuit packaging sample extraction matches all requirements`() {
        val ocrSample = """
            COCONUT CRUNCHY COOKIES
            MRP 10.00
            68 g +16 g EXTRA = 84 g
            NET WEIGHT
            PARLE BISCUITS PVT LTD
            CONSUMER CARE CELL: PARLE BISCUITS PVT LTD
            PHONE NO.: 1800 209 6929
            E-MAIL: CS@PARLE.BIZ
            LIC. No.: 10012051000117
            MFD BY
            BATCH:
            USE BY:
            FOR SALE IN INDIA ONLY
        """.trimIndent()

        val result = ProductFieldExtractor.extract(ocrSample)

        // 1. Product Name: COCONUT CRUNCHY COOKIES
        assertEquals("COCONUT CRUNCHY COOKIES", result.productName)

        // 2. Brand: PARLE (from known brands / manufacturer name, not ingredients)
        assertEquals("PARLE", result.brand)

        // 3. MRP: 10.00 normalized to ₹10.00
        assertEquals("₹10.00", result.mrp)

        // 4. Net Quantity: 84 g from "68 g +16 g EXTRA = 84 g"
        assertEquals("84 g", result.netQuantity)

        // 5. License: 10012051000117 following LIC. No.
        assertEquals("10012051000117", result.licenseNumber)

        // 6. Batch: Must NOT become "USE", must be null when empty
        assertNull("Batch must not extract 'USE' from 'USE BY:'", result.batchNumber)

        // 7. Manufacturer: PARLE BISCUITS PVT LTD
        assertEquals("PARLE BISCUITS PVT LTD", result.manufacturer)

        // 8. Consumer care details detected
        assertNotNull(result.confidenceMap["Consumer Care"])
        assertTrue(result.confidenceMap["Consumer Care"] != "Not detected")
    }

    @Test
    fun `false positive prevention rejects ingredient names and nutrition numbers`() {
        val ocrWithIngredientsAndNutrition = """
            INGREDIENTS: REFINED WHEAT FLOUR (MAIDA), INVERT SUGAR SYRUP, COCONUT (3%)
            NUTRITIONAL INFORMATION PER 100g:
            ENERGY 480 KCAL
            PROTEIN 6.5 g
            CARBOHYDRATE 74.6 g
            TOTAL SUGARS 25.9 g
            ADDED SUGARS 24.1 g
            TOTAL FAT 16.1 g
            BATCH:
            USE BY:
        """.trimIndent()

        val result = ProductFieldExtractor.extract(ocrWithIngredientsAndNutrition)

        // Product Name must NEVER become "COCONUT (3%)" or ingredient line
        assertTrue(result.productName != "COCONUT (3%)")
        assertNull("Product name should reject ingredient list line", result.productName)

        // Brand must NOT become "INVERT"
        assertTrue("Brand must not become INVERT", result.brand != "INVERT")

        // MRP must NOT pick nutrition numbers like 25.9 or 16.1
        assertNull("MRP must not be detected from nutritional table numbers", result.mrp)

        // Net quantity must NOT pick nutrition numbers like 74.6 g or 16.1 g
        assertNull("Net quantity must not pick nutritional weight values", result.netQuantity)

        // Batch must NOT become "USE"
        assertNull("Batch must not become USE from USE BY:", result.batchNumber)
    }

    @Test
    fun `brand falls back to null or Needs review when confidence is low`() {
        val unknownBrandText = """
            CRUNCHY BISCUITS
            MRP: ₹20.00
            NET WT: 150 g
        """.trimIndent()

        val result = ProductFieldExtractor.extract(unknownBrandText)
        // With no known brand and no manufacturer, brand should be null rather than guessing
        assertNull(result.brand)
        assertEquals("Not detected", result.confidenceMap["Brand"])
    }

    @Test
    fun `regression test 1 - MRP 10_00 yields ₹10_00`() {
        val input = "MRP 10.00"
        val result = ProductFieldExtractor.extract(input)
        assertEquals("₹10.00", result.mrp)
    }

    @Test
    fun `regression test 2 - MRP 10_00 with ADDED SUGARS 25_9 yields MRP ₹10_00`() {
        val input = """
            MRP 10.00
            ADDED SUGARS 25.9
        """.trimIndent()
        val result = ProductFieldExtractor.extract(input)
        assertEquals("₹10.00", result.mrp)
    }

    @Test
    fun `regression test 3 - NUTRITIONAL lines are NEVER extracted as manufacturer`() {
        val input = """
            NUTRITIONAL INFORMATION
            PROTEIN
            CARBOHYDRATE
            FAT
        """.trimIndent()
        val result = ProductFieldExtractor.extract(input)
        assertNull("Manufacturer must not be any nutritional line", result.manufacturer)
    }

    @Test
    fun `regression test 4 - MFD BY PARLE BISCUITS PVT LTD yields PARLE BISCUITS PVT LTD`() {
        val input = """
            MFD BY:
            PARLE BISCUITS PVT LTD
        """.trimIndent()
        val result = ProductFieldExtractor.extract(input)
        assertEquals("PARLE BISCUITS PVT LTD", result.manufacturer)
    }

    @Test
    fun `regression test 5 - BATCH USE BY yields Not detected`() {
        val input = """
            BATCH:
            USE BY:
        """.trimIndent()
        val result = ProductFieldExtractor.extract(input)
        assertNull("Batch must not be detected from empty BATCH followed by USE BY", result.batchNumber)
        assertEquals("Not detected", result.confidenceMap["Batch Number"])
    }

    @Test
    fun `regression test 6 - BATCH ABC123 yields ABC123`() {
        val input = """
            BATCH:
            ABC123
        """.trimIndent()
        val result = ProductFieldExtractor.extract(input)
        assertEquals("ABC123", result.batchNumber)
    }

    @Test
    fun `regression test 7 - factory unit code prefixes are stripped for contextual manufacturers`() {
        val input = """
            MFD BY:
            A1)-AJMER FOOD PRODUCTS PVT LTD
            (KR)-AMBAJI FOODS (INDIA) PVT LTD
            (NG)-SHIVAM FOODS
        """.trimIndent()
        val result = ProductFieldExtractor.extract(input)
        assertNotNull(result.manufacturer)
        val validOptions = listOf(
            "AJMER FOOD PRODUCTS PVT LTD",
            "AMBAJI FOODS (INDIA) PVT LTD",
            "SHIVAM FOODS"
        )
        assertTrue(
            "Extracted manufacturer '${result.manufacturer}' must be one of the factory units with prefix stripped",
            validOptions.any { result.manufacturer!!.contains(it) }
        )
    }

    @Test
    fun `regression test 8 - MRP with unit sale price does not extract first digit`() {
        val input = "MRP 10.00 (*0.15/g)"
        val result = ProductFieldExtractor.extract(input)
        assertEquals("₹10.00", result.mrp)
    }

    @Test
    fun `extracts all fields from GS Tea package label correctly`() {
        val gsTeaOcr = """
            GS
            GOLDEN SUPER CTC DUST
            TEA
            Unit Sale Price : Rs. 0.20/g
            M.R.P.20.00/-
            (INCLUSIVE OF ALL TAXES)
            DATE OF PKG. 05/2026
            USE BY 11/2026
            BATCH NO B-42
            NET WEIGHT 100g
            LIC NO. 10015042000123
        """.trimIndent()

        val result = ProductFieldExtractor.extract(gsTeaOcr)

        assertEquals("GS", result.brand)
        assertTrue(
            "Product name should be either 'GOLDEN SUPER CTC DUST' or 'TEA', but was '${result.productName}'",
            result.productName == "GOLDEN SUPER CTC DUST" || result.productName == "TEA"
        )
        assertTrue("Product name must not be Unit Sale Price", result.productName != "Unit Sale Price")
        assertEquals("₹20.00", result.mrp)
        assertEquals("05/2026", result.manufacturingDate)
        assertEquals("11/2026", result.expiryDate)
        assertEquals("B-42", result.batchNumber)
        assertEquals("100 g", result.netQuantity)
        assertEquals("10015042000123", result.licenseNumber)
        assertEquals("Detected", result.confidenceMap["Brand"])
        assertEquals("Detected", result.confidenceMap["MRP"])
        assertEquals("Detected", result.confidenceMap["Manufacturing Date"])
        assertEquals("Detected", result.confidenceMap["Expiry Date"])
        assertEquals("Detected", result.confidenceMap["Batch Number"])
        assertEquals("Detected", result.confidenceMap["Net Quantity"])
        assertEquals("Detected", result.confidenceMap["License Number"])
    }

    @Test
    fun `extracts TEA as product name when single word and PKD with Lic No`() {
        val ocr = """
            GS
            TEA
            Unit Sale Price : Rs. 0.20/g
            M.R.P.20.00/-
            INCLUSIVE OF ALL TAXES
            PKD 05/2026
            USE BY 11/2026
            BATCH NO: B-42
            NET WEIGHT: 100 gm
            Lic No. 10015042000123
        """.trimIndent()

        val result = ProductFieldExtractor.extract(ocr)

        assertEquals("GS", result.brand)
        assertEquals("TEA", result.productName)
        assertEquals("₹20.00", result.mrp)
        assertEquals("05/2026", result.manufacturingDate)
        assertEquals("11/2026", result.expiryDate)
        assertEquals("B-42", result.batchNumber)
        assertEquals("100 g", result.netQuantity)
        assertEquals("10015042000123", result.licenseNumber)
    }

    @Test
    fun `product name is never extracted from disallowed phrases`() {
        val disallowed = listOf(
            "Unit Sale Price",
            "Prg",
            "MRP",
            "Ingredients",
            "Net Weight",
            "Batch No",
            "Date of Pkg",
            "Use By",
            "Customer Care",
            "Other",
            "Inclusive of all Taxes"
        )
        for (phrase in disallowed) {
            val result = ProductFieldExtractor.extract(phrase)
            assertNull("Phrase '$phrase' must not be extracted as product name", result.productName)
        }
    }

    @Test
    fun `insufficient confidence returns Not detected without inventing values`() {
        val unrecognisedText = "XYZ FOOBAR BAZ"
        val result = ProductFieldExtractor.extract(unrecognisedText)

        assertNull(result.mrp)
        assertNull(result.manufacturingDate)
        assertNull(result.expiryDate)
        assertNull(result.batchNumber)
        assertNull(result.netQuantity)
        assertEquals("Not detected", result.confidenceMap["MRP"])
        assertEquals("Not detected", result.confidenceMap["Manufacturing Date"])
        assertEquals("Not detected", result.confidenceMap["Expiry Date"])
        assertEquals("Not detected", result.confidenceMap["Batch Number"])
    }
}
