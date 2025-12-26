package com.eisiadev.enceladus.treeplugin

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class TreePlugin : JavaPlugin() {
    lateinit var regenManager: TreeRegenManager
    lateinit var dataFile: File

    override fun onEnable() {
        // 데이터 폴더 생성
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        dataFile = File(dataFolder, "regen_queue.json")

        // 매니저 초기화
        regenManager = TreeRegenManager(this)

        // 이벤트 리스너 등록
        server.pluginManager.registerEvents(TreeBreakListener(this), this)

        // 서버 시작 시 미완료 리젠 작업 로드
        regenManager.loadRegenQueue()

        logger.info("TreePlugin enabled!")
    }

    override fun onDisable() {
        // 리젠 큐 저장
        regenManager.saveRegenQueue()
        logger.info("TreePlugin disabled!")
    }
}