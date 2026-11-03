package com.shakeflash.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * shadcn/ui "zinc" dark palette, adapted for a flashlight app: near-black surfaces,
 * hairline borders, white primary actions and one warm amber accent for the torch.
 */
object SFColors {
    val Background = Color(0xFF09090B)
    val Surface = Color(0xFF101012)
    val SurfaceElevated = Color(0xFF18181B)
    val SurfaceHighlight = Color(0xFF1F1F23)
    val Border = Color(0xFF27272A)
    val BorderStrong = Color(0xFF3F3F46)

    val Foreground = Color(0xFFFAFAFA)
    val Muted = Color(0xFFA1A1AA)
    val Subtle = Color(0xFF71717A)

    val OnPrimary = Color(0xFF09090B)
    val Primary = Color(0xFFFAFAFA)

    val Accent = Color(0xFFFBBF24)
    val AccentLight = Color(0xFFFDE68A)
    val AccentSoft = Color(0xFF2A2210)

    val Success = Color(0xFF4ADE80)
    val SuccessSoft = Color(0xFF0F2A1A)
    val Danger = Color(0xFFF87171)
    val DangerSoft = Color(0xFF2E1414)
}
