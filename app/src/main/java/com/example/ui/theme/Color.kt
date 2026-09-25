package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// SmartVerify SIH Color Palette — Centralized Color Definitions
// ALL colors in the app should derive from this file.
// Do NOT hardcode colors in individual screens.
// =============================================================================

// --- Primary Brand Colors (Dark Green Theme) ---
val Primary = Color(0xFF065F46)         // Deep forest / dark emerald green (#065F46)
val PrimaryDark = Color(0xFF047857)     // Darker shade for dark theme
val PrimaryLight = Color(0xFF34D399)    // Mint emerald for dark theme primary
val PrimaryContainer = Color(0xFFD1FAE5)        // Soft sage/mint container
val PrimaryContainerDark = Color(0xFF064E3B)    // Deep forest container

// --- Secondary Colors ---
val Secondary = Color(0xFF0F766E)               // Deep teal-pine (#0F766E)
val SecondaryDark = Color(0xFF2DD4BF)           // Lighter teal for dark
val SecondaryContainer = Color(0xFFCCFBF1)
val SecondaryContainerDark = Color(0xFF134E4A)

// --- Accent / Tertiary Colors ---
val Accent = Color(0xFF10B981)                  // Vibrant emerald (#10B981)
val AccentDark = Color(0xFF6EE7B7)              // Mint accent for dark
val AccentContainer = Color(0xFFA7F3D0)
val AccentContainerDark = Color(0xFF065F46)

// --- Background & Surface ---
val Background = Color(0xFFF4F8F5)              // Crisp white with subtle green tint (#F4F8F5)
val BackgroundDark = Color(0xFF09140E)          // Deep dark green-slate background
val Surface = Color(0xFFFFFFFF)                 // Pure white surface
val SurfaceDark = Color(0xFF11241A)             // Deep dark-green surface
val SurfaceVariant = Color(0xFFE8F2EC)          // Soft pale green surface
val SurfaceVariantDark = Color(0xFF1A3326)

// --- On Colors (text/icon on colored surfaces) ---
val OnPrimary = Color.White
val OnPrimaryDark = Color(0xFF0F172A)
val OnSecondary = Color.White
val OnBackground = Color(0xFF0F172A)            // Near-black text (#0F172A)
val OnBackgroundDark = Color(0xFFF8FAFC)
val OnSurface = Color(0xFF0F172A)
val OnSurfaceDark = Color(0xFFF8FAFC)
val OnSurfaceVariant = Color(0xFF475569)        // Muted text
val OnSurfaceVariantDark = Color(0xFFCBD5E1)
val Outline = Color(0xFFCBD5E1)                 // Border color (light)
val OutlineDark = Color(0xFF475569)

// --- Semantic Status Colors ---
// These are used for product verification UI — NOT the primary blue
val StatusVerified = Color(0xFF16A34A)          // Green (#16A34A)
val StatusVerifiedContainer = Color(0xFFDCFCE7) // Light green background
val StatusVerifiedText = Color(0xFF14532D)      // Dark green text

val StatusWarning = Color(0xFFF59E0B)           // Amber (#F59E0B)
val StatusWarningContainer = Color(0xFFFEF3C7)  // Light amber background
val StatusWarningText = Color(0xFF78350F)       // Dark amber text

val StatusInvalid = Color(0xFFDC2626)           // Red (#DC2626)
val StatusInvalidContainer = Color(0xFFFEE2E2)  // Light red background
val StatusInvalidText = Color(0xFF7F1D1D)       // Dark red text

val StatusUnknown = Color(0xFF6B7280)           // Gray
val StatusUnknownContainer = Color(0xFFF3F4F6)
val StatusUnknownText = Color(0xFF374151)

// --- Error Colors (M3 error slot) ---
val Error = Color(0xFFDC2626)                   // Red (#DC2626)
val ErrorDark = Color(0xFFFF6B6B)
val ErrorContainer = Color(0xFFFEE2E2)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnError = Color.White
val OnErrorDark = Color(0xFF0F172A)
val OnErrorContainer = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

// --- Success (custom slot, used via CompositionLocal or explicit reference) ---
val Success = Color(0xFF16A34A)                 // Green (#16A34A)
val SuccessDark = Color(0xFF4ADE80)

// --- Slate Scale (used for misc UI elements) ---
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate400 = Color(0xFF94A3B8)
val Slate600 = Color(0xFF475569)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
