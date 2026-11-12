package com.safari.zyra.core.ui.theme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
object ZyraSpacing { val xxs=2.dp; val xs=6.dp; val sm=10.dp; val md=16.dp; val lg=24.dp; val xl=32.dp }
object ZyraRadius { val small=RoundedCornerShape(10.dp); val medium=RoundedCornerShape(16.dp); val card=RoundedCornerShape(22.dp); val large=RoundedCornerShape(28.dp); val pill=RoundedCornerShape(50) }
object ZyraMotion { const val fast=160; const val normal=260; const val slow=420 }
val ZyraShapes=Shapes(extraSmall=ZyraRadius.small, small=ZyraRadius.small, medium=ZyraRadius.medium, large=ZyraRadius.card, extraLarge=ZyraRadius.large)
