/*
 * Copyright (c) 2021 NetEase, Inc.  All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.nimflutter.services

import android.content.Context
import android.text.TextUtils
import com.netease.nimflutter.*
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.Observer
import com.netease.nimlib.sdk.RequestCallback
import com.netease.nimlib.sdk.nos.NosService
import com.netease.nimlib.sdk.nos.NosServiceObserve
import com.netease.nimlib.sdk.nos.model.NosTransferInfo
import com.netease.nimlib.sdk.nos.model.NosTransferProgress
import com.netease.yunxin.kit.alog.ALog
import java.io.File

class FLTNOSService(
    applicationContext: Context,
    nimCore: NimCore,
) : FLTService(applicationContext, nimCore) {

    private val tag = "FLTNOSService"

    override val serviceName = "NOSService"

    override fun onMethodCalled(method: String, arguments: Map<String, *>, safeResult: SafeResult) {
        when (method) {
            "upload" -> upload(arguments, ResultCallback(safeResult))
            else -> safeResult.notImplemented()
        }
    }

    private val nosTransferProgress =
        Observer { nosTransferProgress: NosTransferProgress ->
            run {
                notifyEvent(
                    "onNOSTransferProgress",
                    mutableMapOf("progress" to
                            nosTransferProgress.transferred.toDouble() / nosTransferProgress.total)
                )
            }
        }

    private val nosTransferStatus =
        Observer { nosTransferInfo: NosTransferInfo ->
            run {
                notifyEvent(
                    "onNOSTransferStatus",
                    nosTransferInfo.toMap() as MutableMap<String, Any?>
                )
            }
        }

    init {
        nimCore.onInitialized {
            NIMClient.getService(NosServiceObserve::class.java)
                .observeNosTransferProgress(nosTransferProgress, true)
            NIMClient.getService(NosServiceObserve::class.java)
                .observeNosTransferStatus(nosTransferStatus, true)
        }
    }

    /**
     * 上传文件
     */
    private fun upload(
        arguments: Map<String, *>,
        resultCallback: ResultCallback<String>
    ) {
        val filePath = arguments["filePath"] as? String
        if (filePath == null) {
            resultCallback.result(
                NimResult(code = -1, errorDetails = "upload but the filePath is empty!")
            )
        }else {
            val file = File(filePath)
            if (!file.exists()) {
                resultCallback.result(
                    NimResult(code = -1, errorDetails = "upload but the file is not exists!")
                )
                return
            }
            val mimeType = arguments["mimeType"] as? String ?: "image/jpeg"
            val sceneKey = arguments["sceneKey"] as? String
            if(!TextUtils.isEmpty(sceneKey)){
                NIMClient.getService(NosService::class.java).uploadAtScene(file, mimeType, sceneKey)
                    .setCallback(object : RequestCallback<String> {
                        override fun onSuccess(param: String?) {
                            ALog.d(tag, "uploadAtScene onSuccess")
                            resultCallback.result(NimResult(code = 0,data = param))
                        }

                        override fun onFailed(code: Int) {
                            onFailed("uploadAtScene onFailed", code, resultCallback)
                        }

                        override fun onException(exception: Throwable?) {
                            onException("uploadAtScene onException", exception, resultCallback)
                        }
                    })
            }else {
                NIMClient.getService(NosService::class.java).upload(file, mimeType)
                    .setCallback(object : RequestCallback<String> {
                        override fun onSuccess(param: String?) {
                            ALog.d(tag, "upload onSuccess")
                            resultCallback.result(NimResult(code = 0,data = param))
                        }

                        override fun onFailed(code: Int) {
                            onFailed("upload onFailed", code, resultCallback)
                        }

                        override fun onException(exception: Throwable?) {
                            onException("upload onException", exception, resultCallback)
                        }
                    })
            }
        }
    }
}