package com.coelacanth9.simplecustomlauncher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * パイメニューの各項目データ
 */
data class ExpandableMenuItem(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val onSelect: () -> Unit,
    val iconSize: Dp = 24.dp,
    val itemSize: Dp = 56.dp,
    val labelStyle: TextStyle? = null,
    val showLabel: Boolean = true,
)

/**
 * メニューの展開方向
 */
enum class ExpandDirection {
    /** 左上に展開（FABが右下にある場合） */
    TopStart,
    /** 右上に展開（FABが左下にある場合） */
    TopEnd,
    /** 左下に展開（FABが右上にある場合） */
    BottomStart,
    /** 右下に展開（FABが左上にある場合） */
    BottomEnd,
}

/**
 * 長押しパイメニュー付きFAB
 *
 * - タップ → [onClick] 実行
 * - 長押しホールド → [menuItems] が弧状にフェードイン
 * - 指をスライド → 近い項目がハイライト
 * - 指を離す → ハイライト中の項目の [ExpandableMenuItem.onSelect] を実行（何もない場所ならキャンセル）
 */
@Composable
fun FloatingExpandableButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    menuItems: List<ExpandableMenuItem>,
    modifier: Modifier = Modifier,
    expandDirection: ExpandDirection = ExpandDirection.TopStart,
    menuRadius: Dp = 140.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableIntStateOf(-1) }

    var fabCenterX by remember { mutableFloatStateOf(0f) }
    var fabCenterY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    val radiusPx = with(density) { menuRadius.toPx() }
    val hitRadiusPx = with(density) { 40.dp.toPx() }

    val menuAngles = remember(menuItems.size, expandDirection) {
        calculateMenuAngles(menuItems.size, expandDirection)
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "expandProgress"
    )

    val highlightScales = menuItems.indices.map { index ->
        animateFloatAsState(
            targetValue = if (highlightedIndex == index) 1.2f else 1f,
            animationSpec = spring(dampingRatio = 0.6f),
            label = "highlightScale$index"
        )
    }

    Box(modifier = modifier) {
        if (expandProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(expandProgress * 0.3f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }

        if (expandProgress > 0f && fabCenterX > 0f) {
            menuItems.forEachIndexed { index, item ->
                val angleRad = Math.toRadians(menuAngles[index])
                val targetX = fabCenterX + (radiusPx * cos(angleRad)).toFloat() * expandProgress
                val targetY = fabCenterY - (radiusPx * sin(angleRad)).toFloat() * expandProgress

                val itemAlpha = expandProgress
                val itemScale = highlightScales[index].value
                val isHighlighted = highlightedIndex == index
                val itemSizePx = with(density) { item.itemSize.toPx() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(0, 0) {
                                val x = (targetX - placeable.width / 2f).roundToInt()
                                val y = (targetY - placeable.height + itemSizePx / 2f).roundToInt()
                                placeable.place(x, y)
                            }
                        }
                        .alpha(itemAlpha)
                        .scale(itemScale)
                ) {
                    if (item.showLabel) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = item.label,
                                style = item.labelStyle ?: TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                ),
                                color = if (isHighlighted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isHighlighted)
                            item.containerColor
                        else
                            item.containerColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(item.itemSize),
                        shadowElevation = if (isHighlighted) 8.dp else 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = item.contentColor,
                                modifier = Modifier.size(item.iconSize)
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { /* pointerInput で処理するため空 */ },
            shape = CircleShape,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionInParent()
                    val size = coordinates.size
                    fabCenterX = pos.x + size.width / 2f
                    fabCenterY = pos.y + size.height / 2f
                }
                .pointerInput(menuItems.size, expandDirection) {
                    awaitPointerEventScope {
                        while (true) {
                            val downEvent = awaitPointerEvent()
                            if (downEvent.type != PointerEventType.Press) continue
                            val downChange = downEvent.changes.firstOrNull() ?: continue
                            downChange.consume()

                            var released = false
                            val upChange = withTimeoutOrNull(
                                viewConfiguration.longPressTimeoutMillis
                            ) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.all { !it.pressed }) {
                                        released = true
                                        return@withTimeoutOrNull event.changes.first()
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE")
                                null
                            }

                            if (released && upChange != null) {
                                onClick()
                            } else {
                                isExpanded = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                var prevHighlighted = -1

                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }

                                    if (event.changes.all { !it.pressed }) {
                                        if (highlightedIndex >= 0 && highlightedIndex < menuItems.size) {
                                            menuItems[highlightedIndex].onSelect()
                                        }
                                        isExpanded = false
                                        highlightedIndex = -1
                                        break
                                    }

                                    val pos = event.changes.first().position
                                    val fabSize = size
                                    val relX = pos.x - fabSize.width / 2f
                                    val relY = pos.y - fabSize.height / 2f

                                    highlightedIndex = calculateHighlightedItem(
                                        fingerRelative = Offset(relX, relY),
                                        menuAngles = menuAngles,
                                        radiusPx = radiusPx,
                                        hitRadiusPx = hitRadiusPx,
                                        expandProgress = expandProgress
                                    )

                                    if (highlightedIndex != prevHighlighted && highlightedIndex >= 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    prevHighlighted = highlightedIndex
                                }
                            }
                        }
                    }
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    }
}

/**
 * メニュー項目の角度を計算する
 * 角度は数学の標準: 右=0°, 上=90°, 左=180°, 下=270°
 */
private fun calculateMenuAngles(itemCount: Int, direction: ExpandDirection): List<Double> {
    if (itemCount == 0) return emptyList()

    val centerAngle = when (direction) {
        ExpandDirection.TopStart -> 135.0
        ExpandDirection.TopEnd -> 45.0
        ExpandDirection.BottomStart -> 225.0
        ExpandDirection.BottomEnd -> 315.0
    }

    val arcSpan = when (itemCount) {
        1 -> 0.0
        2 -> 50.0
        3 -> 80.0
        4 -> 110.0
        else -> 140.0
    }

    if (itemCount == 1) return listOf(centerAngle)

    val startAngle = centerAngle - arcSpan / 2
    val step = arcSpan / (itemCount - 1)

    return (0 until itemCount).map { i ->
        startAngle + step * i
    }
}

/**
 * 指の位置から最も近いメニュー項目を判定する
 */
private fun calculateHighlightedItem(
    fingerRelative: Offset,
    menuAngles: List<Double>,
    radiusPx: Float,
    hitRadiusPx: Float,
    expandProgress: Float,
): Int {
    var closestIndex = -1
    var closestDistance = Float.MAX_VALUE

    menuAngles.forEachIndexed { index, angleDeg ->
        val angleRad = Math.toRadians(angleDeg)
        val itemX = (radiusPx * cos(angleRad)).toFloat() * expandProgress
        val itemY = -(radiusPx * sin(angleRad)).toFloat() * expandProgress

        val dx = fingerRelative.x - itemX
        val dy = fingerRelative.y - itemY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < hitRadiusPx && distance < closestDistance) {
            closestDistance = distance
            closestIndex = index
        }
    }

    return closestIndex
}
