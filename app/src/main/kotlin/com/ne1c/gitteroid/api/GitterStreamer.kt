package com.ne1c.gitteroid.api

import com.google.gson.Gson
import com.ne1c.gitteroid.models.data.MessageModel
import rx.Observable
import rx.Observer
import rx.observables.SyncOnSubscribe
import java.io.BufferedReader
import java.io.IOException

class GitterStreamer(private val mStreamApi: GitterStreamApi,
                     private val bearer: String) {
    private val STREAM_URL = "https://stream.gitter.im/v1/rooms/%s/chatMessages"

    fun getMessageStream(roomId: String): Observable<MessageModel> {
        val streamUrl = String.format(STREAM_URL, roomId)
        return mStreamApi.getMessagesStream(streamUrl, bearer)
                .flatMap({ Observable.create(OnSubscribeBufferedReader(BufferedReader(it.charStream()))) })
                .filter { it != null && !it.trim { it <= ' ' }.isEmpty() }
                .map({ Gson().fromJson<MessageModel>(it, MessageModel::class.java) })
    }

    private inner class OnSubscribeBufferedReader(private val mReader: BufferedReader) : SyncOnSubscribe<BufferedReader, String>() {
        override fun generateState(): BufferedReader {
            return mReader
        }

        override fun next(state: BufferedReader, observer: Observer<in String>): BufferedReader {
            val line: String?
            try {
                line = state.readLine()
                if (line == null) {
                    observer.onCompleted()
                } else {
                    observer.onNext(line)
                }
            } catch (e: IOException) {
                try {
                    state.close()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }

                observer.onError(e)
            }

            return state
        }
    }
}
