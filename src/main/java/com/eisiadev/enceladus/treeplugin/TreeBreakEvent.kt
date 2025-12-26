package com.eisiadev.enceladus.treeplugin

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TreeBreakEvent(
    private val player: Player,
    private val block: Block,
    private val treeBlocks: List<Block>
) : Event(), Cancellable {

    private var cancelled = false
    private var regenTime: Long = 6000L // 기본 5분 (틱 단위)

    fun getPlayer(): Player = player
    fun getBlock(): Block = block
    fun getTreeBlocks(): List<Block> = treeBlocks
    fun getTreeBlockCount(): Int = treeBlocks.size // 부순 블록 개수

    fun getRegenTime(): Long = regenTime
    fun setRegenTime(ticks: Long) {
        regenTime = ticks
    }

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}