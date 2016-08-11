package com.ne1c.gitteroid.api

import com.google.gson.Gson
import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.utils.Utils

import java.io.BufferedReader
import java.io.IOException

import javax.inject.Inject

import rx.Observable
import rx.Observer
import rx.observables.SyncOnSubscribe

class GitterStreamer
@Inject
constructor(private val mStreamApi: GitterStreamApi) {
    private val STREAM_URL = "https://stream.gitter.im/v1/rooms/%s/chatMessages"

    fun getMessageStream(roomId: String): Observable<MessageModel> {
        val streamUrl = String.format(STREAM_URL, roomId)
        return mStreamApi.getMessagesStream(streamUrl, Utils.instance.bearer).flatMap<String>({ response -> Observable.create(OnSubscribeBufferedReader(BufferedReader(response.charStream()))) }).filter { s -> s != null && !s.trim { it <= ' ' }.isEmpty() }.map<MessageModel>({ s -> Gson().fromJson<MessageModel>(s, MessageModel::class.java) })
    }

    inner class OnSubscribeBufferedReader(private val mReader: BufferedReader) : SyncOnSubscribe<BufferedReader, String>() {


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
