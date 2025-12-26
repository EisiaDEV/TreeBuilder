package com.eisiadev.enceladus.treeplugin

import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class TreeBreakListener(private val plugin: TreePlugin) : Listener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun shutdown() {
        scope.cancel()
    }

    // 모든 원목, 목재, 껍질 벗긴 원목
    private val woodTypes = setOf(
        // 원목
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG,
        // 껍질 벗긴 원목
        Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG,
        Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
        // 목재
        Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
        Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
        // 껍질 벗긴 목재
        Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_BIRCH_WOOD,
        Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
        Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
        // 판자
        Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
        Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
        Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
        Material.CRIMSON_PLANKS, Material.WARPED_PLANKS
    )

    private val leafTypes = setOf(
        Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES
    )

    // 모든 도끼 타입
    private val axeTypes = setOf(
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    )

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player

        // 나무 블록이 아니면 무시
        if (block.type !in woodTypes) return

        // 도끼를 들고 있지 않으면 무시
        if (player.inventory.itemInMainHand.type !in axeTypes) return

        // 이벤트 취소 (플러그인이 직접 처리)
        event.isCancelled = true

        // 비동기로 나무 탐색
        scope.launch {
            val treeBlocks = findConnectedTree(block)

            // 탐색된 나무 블록들 주변에만 나뭇잎 체크
            if (!hasLeavesNearTree(treeBlocks)) {
                return@launch // 건축물로 판단
            }

            // 메인 스레드에서 TreeBreakEvent 발생
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val treeEvent = TreeBreakEvent(player, block, treeBlocks)
                Bukkit.getPluginManager().callEvent(treeEvent)

                if (!treeEvent.isCancelled) {
                    // 나무 파괴 및 리젠 등록
                    plugin.regenManager.breakAndScheduleRegen(
                        treeBlocks,
                        treeEvent.getRegenTime()
                    )
                }
            })
        }
    }

    // BFS로 연결된 나무 블록 찾기
    private fun findConnectedTree(start: Block): List<Block> {
        val found = mutableSetOf<Block>()
        val queue = ArrayDeque<Block>()
        queue.add(start)
        found.add(start)

        while (queue.isNotEmpty() && found.size < 500) { // 최대 500블록
            val current = queue.removeFirst()

            // 인접 26블록 체크
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue

                        val neighbor = current.getRelative(dx, dy, dz)

                        if (neighbor !in found && neighbor.type in woodTypes) {
                            found.add(neighbor)
                            queue.add(neighbor)
                        }
                    }
                }
            }
        }

        return found.toList()
    }

    // 탐색된 나무 블록들 주변에만 나뭇잎 체크
    private fun hasLeavesNearTree(treeBlocks: List<Block>): Boolean {
        for (block in treeBlocks) {
            for (dx in -3..3) {
                for (dy in -3..3) {
                    for (dz in -3..3) {
                        val relative = block.getRelative(dx, dy, dz)
                        if (relative.type in leafTypes) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}