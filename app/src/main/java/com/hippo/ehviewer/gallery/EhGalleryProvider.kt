/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.image.Image
import com.hippo.unifile.UniFile
import com.hippo.yorozuya.SimpleHandler
import java.util.Locale

class EhGalleryProvider(private val mGalleryInfo: GalleryInfo) : GalleryProvider2(),
    OnSpiderListener {
    private var mSpiderQueen: SpiderQueen? = null
    override fun start() {
        super.start()
        mSpiderQueen = SpiderQueen.obtainSpiderQueen(mGalleryInfo, SpiderQueen.MODE_READ)
        mSpiderQueen!!.addOnSpiderListener(this)
    }

    override fun stop() {
        super.stop()
        if (mSpiderQueen != null) {
            mSpiderQueen!!.removeOnSpiderListener(this)
            // Activity recreate may called, so wait 3000s
            SimpleHandler.getInstance().postDelayed(ReleaseTask(mSpiderQueen), 3000)
            mSpiderQueen = null
        }
    }

    override fun getStartPage(): Int {
        return if (mSpiderQueen != null) {
            mSpiderQueen!!.startPage
        } else {
            super.getStartPage()
        }
    }

    override fun getImageFilename(index: Int): String {
        return String.format(
            Locale.US,
            "%d-%s-%08d",
            mGalleryInfo.gid,
            mGalleryInfo.token,
            index + 1
        )
    }

    override fun getImageFilenameWithExtension(index: Int): String {
        if (null != mSpiderQueen) {
            val extension = mSpiderQueen!!.getExtension(index)
            if (extension != null) {
                return String.format(
                    Locale.US,
                    "%d-%s-%08d.%s",
                    mGalleryInfo.gid,
                    mGalleryInfo.token,
                    index + 1,
                    extension
                )
            }
        }
        return String.format(
            Locale.US,
            "%d-%s-%08d",
            mGalleryInfo.gid,
            mGalleryInfo.token,
            index + 1
        )
    }

    override fun save(index: Int, file: UniFile): Boolean {
        return if (null != mSpiderQueen) {
            mSpiderQueen!!.save(index, file)
        } else {
            false
        }
    }

    override fun save(index: Int, dir: UniFile, filename: String): UniFile? {
        return if (null != mSpiderQueen) {
            mSpiderQueen!!.save(index, dir, filename)
        } else {
            null
        }
    }

    override fun putStartPage(page: Int) {
        if (mSpiderQueen != null) {
            mSpiderQueen!!.putStartPage(page)
        }
    }

    override fun size(): Int {
        return if (mSpiderQueen != null) {
            mSpiderQueen!!.size()
        } else {
            STATE_ERROR
        }
    }

    override fun onRequest(index: Int) {
        if (mSpiderQueen != null) {
            when (val `object` = mSpiderQueen!!.request(index)) {
                is Float -> {
                    notifyPagePercent(index, `object`)
                }

                is String -> {
                    notifyPageFailed(index, `object`)
                }

                null -> {
                    notifyPageWait(index)
                }
            }
        }
    }

    override fun onForceRequest(index: Int) {
        if (mSpiderQueen != null) {
            when (val `object` = mSpiderQueen!!.forceRequest(index)) {
                is Float -> {
                    notifyPagePercent(index, `object`)
                }

                is String -> {
                    notifyPageFailed(index, `object`)
                }

                null -> {
                    notifyPageWait(index)
                }
            }
        }
    }

    override fun onCancelRequest(index: Int) {
        if (mSpiderQueen != null) {
            mSpiderQueen!!.cancelRequest(index)
        }
    }

    override fun getError(): String? {
        return if (mSpiderQueen != null) {
            mSpiderQueen!!.error
        } else {
            "Error" // TODO
        }
    }

    override fun onGetPages(pages: Int) {
        notifyDataChanged()
    }

    override fun onGet509(index: Int) {
        // TODO
    }

    override fun onPageDownload(
        index: Int,
        contentLength: Long,
        receivedSize: Long,
        bytesRead: Int
    ) {
        if (contentLength > 0) {
            notifyPagePercent(index, receivedSize.toFloat() / contentLength)
        }
    }

    override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) {
        notifyDataChanged(index)
    }

    override fun onPageFailure(
        index: Int,
        error: String,
        finished: Int,
        downloaded: Int,
        total: Int
    ) {
        notifyPageFailed(index, error)
    }

    override fun onFinish(finished: Int, downloaded: Int, total: Int) {}
    override fun onGetImageSuccess(index: Int, image: Image) {
        notifyPageSucceed(index, image)
    }

    override fun onGetImageFailure(index: Int, error: String) {
        notifyPageFailed(index, error)
    }

    private class ReleaseTask(private var mSpiderQueen: SpiderQueen?) : Runnable {
        override fun run() {
            if (null != mSpiderQueen) {
                SpiderQueen.releaseSpiderQueen(mSpiderQueen!!, SpiderQueen.MODE_READ)
                mSpiderQueen = null
            }
        }
    }
}