package com.eisiadev.enceladus.treeplugin

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class SavedBlockData(
    val x: Int,
    val y: Int,
    val z: Int,
    val world: String,
    val blockDataString: String // BlockData를 String으로 직렬화
)

data class RegenTask(
    val blocks: List<SavedBlockData>,
    val regenTime: Long, // 틱
    val startTick: Long
)

class TreeRegenManager(private val plugin: TreePlugin) {

    private val regenQueue = mutableListOf<RegenTask>()
    private var regenTaskId: BukkitTask? = null
    private val gson = Gson()

    init {
        // 1틱마다 리젠 체크
        regenTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            processRegenQueue()
        }, 0L, 1L)
    }

    fun breakAndScheduleRegen(blocks: List<Block>, regenTime: Long) {
        val blockDataList = mutableListOf<SavedBlockData>()

        // 동기적으로 블록 저장 및 배리어로 치환
        for (block in blocks) {
            // BlockData를 String으로 저장 (방향 정보 포함)
            val blockDataString = block.blockData.asString

            blockDataList.add(
                SavedBlockData(
                    block.x,
                    block.y,
                    block.z,
                    block.world.name,
                    blockDataString
                )
            )
            block.type = Material.BARRIER
        }

        // 리젠 큐에 추가
        regenQueue.add(
            RegenTask(
                blockDataList,
                regenTime,
                Bukkit.getCurrentTick().toLong()
            )
        )
    }

    private fun processRegenQueue() {
        val currentTick = Bukkit.getCurrentTick()
        val toRemove = mutableListOf<RegenTask>()

        for (task in regenQueue) {
            val elapsed = currentTick - task.startTick

            if (elapsed >= task.regenTime) {
                // 리젠 실행
                regenTree(task.blocks.toMutableList())
                toRemove.add(task)
            }
        }

        regenQueue.removeAll(toRemove)
    }

    private fun regenTree(blocks: MutableList<SavedBlockData>) {
        if (blocks.isEmpty()) return

        // 한 번에 10블록씩 복구 (렉 방지)
        var count = 0
        val iterator = blocks.iterator()

        while (iterator.hasNext() && count < 10) {
            val blockData = iterator.next()
            val world = Bukkit.getWorld(blockData.world) ?: continue
            val block = world.getBlockAt(blockData.x, blockData.y, blockData.z)

            if (block.type == Material.BARRIER) {
                try {
                    // String을 BlockData로 복원 (방향 정보 포함)
                    val restoredBlockData = Bukkit.createBlockData(blockData.blockDataString)
                    block.blockData = restoredBlockData
                    count++
                    iterator.remove()
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to restore block data: ${e.message}")
                    iterator.remove()
                }
            } else {
                iterator.remove()
            }
        }

        // 남은 블록이 있으면 다음 틱에 계속
        if (blocks.isNotEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                regenTree(blocks)
            }, 1L)
        }
    }

    fun saveRegenQueue() {
        try {
            FileWriter(plugin.dataFile).use {
                it.write(gson.toJson(regenQueue))
            }
            plugin.logger.info("Saved ${regenQueue.size} regen tasks")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save regen queue: ${e.message}")
        }
    }

    fun loadRegenQueue() {
        if (!plugin.dataFile.exists()) return

        try {
            val type = object : TypeToken<List<RegenTask>>() {}.type
            val loaded: List<RegenTask> = gson.fromJson(FileReader(plugin.dataFile), type)
            regenQueue.addAll(loaded)
            plugin.logger.info("Loaded ${regenQueue.size} regen tasks")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load regen queue: ${e.message}")
        }
    }
}