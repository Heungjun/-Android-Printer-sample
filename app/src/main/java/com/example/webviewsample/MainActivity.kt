package com.example.webviewsample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.*
import android.print.*
import android.print.pdf.PrintedPdfDocument
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private var mWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.sampleWebview)
        myWebView.settings.javaScriptEnabled = true;
//        myWebView.loadUrl("http://192.168.2.113/PDA/hcm000")
        myWebView.loadUrl("http://gmdwns92.dothome.co.kr/")
        myWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
        }
        myWebView.addJavascriptInterface(WebAppInterface(this, myWebView), "Android")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createWebPrintJob(webView: WebView) {
        Log.i("YHJ", "createWebPrintJob: ")
        // Generate an HTML document on the fly:
        val htmlDocument =
            "<html><body><h1>Test Content</h1><p>Testing, testing, testing...</p></body></html>"
//        webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null)

        // Keep a reference to WebView object until you pass the PrintDocumentAdapter
        // to the PrintManager
        mWebView = webView

        Log.i("YHJ", "start...")
        // Get a PrintManager instance
        (this?.getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.let { printManager ->

            val jobName = "${this.getString(R.string.app_name)} Document"

            // Get a print adapter instance
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            // Create a print job with name and adapter instance
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder().build()
            )
//                .also { printJob ->

            // Save the job object for later status checking
//                printJobs += printJob
//            }
        }
        Log.i("YHJ", "end...")
    }

}

class WebAppInterface(private val mContext: Context, private val webView: WebView) {
    /** Show a toast from the web page  */
    @JavascriptInterface
    fun showToast(toast: String) {
        Log.i("YHJ", "LOG Test...")
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JavascriptInterface
    fun printSampleTest(){
        Log.i("YHJ TEST", "Button Click!!")
//        doPrint()

        val mainHandler: Handler = Handler(mContext.mainLooper)
        Log.i("YHJ TEST", "var mainHandler create!")
        var myRunnable: Runnable = Runnable {
            Log.i("YHJ TEST", "Runnable:")
            var callingClassFun: MainActivity = mContext as MainActivity
            callingClassFun.createWebPrintJob(mContext.findViewById(R.id.sampleWebview))
        }
        mainHandler.post(myRunnable)
        Log.i("YHJ TEST", "end printSampleTest")
    }

    private fun doPrint() {
        Log.i("YHJ TEST", "doPrint")
        mContext?.also { context ->
            // Get a PrintManager instance
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            // Set job name, which will be displayed in the print queue
            val jobName = "${context.getString(R.string.app_name)} Document"
            // Start a print job, passing in a PrintDocumentAdapter implementation
            // to handle the generation of a print document
            printManager.print(jobName, MyPrintDocumentAdapter(context), null)
        }
    }



}

private class MyPrintDocumentAdapter(var context: Context) : PrintDocumentAdapter() {
    var mPdfDocument: PrintedPdfDocument? = null
    var pages = 0
    override fun onStart() {
        super.onStart()
    }

    override fun onLayout(
        oldAttributes: PrintAttributes,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        metadata: Bundle
    ) {
        // Create a new PdfDocument with the requested page attributes
        mPdfDocument = PrintedPdfDocument(context, newAttributes)

        // Respond to cancellation request
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        // Compute the expected number of printed pages
//        pages = computePageCount(newAttributes)
        pages = 1;
        if (pages > 0) {
            // Return print information to print framework
            val info = PrintDocumentInfo.Builder("print_output.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pages)
                .build()
            // Content layout reflow is complete
            callback.onLayoutFinished(info, true)
        } else {
            // Otherwise report an error to the print framework
            callback.onLayoutFailed("Page count calculation failed.")
        }
    }

    override fun onWrite(
        pageRanges: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        // Iterate over each page of the document,
        // check if it's in the output range.
        for (i in 0 until pages) {
            // Check to see if this page is in the output range.
            if (containsPage(pageRanges, i)) {
                // If so, add it to writtenPagesArray. writtenPagesArray.size()
                // is used to compute the next output page index.
                //  writtenPagesArray.append(writtenPagesArray.size(), i);   //writtenPagesArray is not referenced anywhere else ma
                //maybe no need to implement it
                val page = mPdfDocument!!.startPage(i)

                // check for cancellation
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                    mPdfDocument!!.close()
                    mPdfDocument = null
                    return
                }

                // Draw page content for printing
                drawPage(page)

                // Rendering is complete, so page can be finalized.
                mPdfDocument!!.finishPage(page)
            }
        }

        // Write PDF document to file
        try {
            mPdfDocument!!.writeTo(
                FileOutputStream(
                    destination.fileDescriptor
                )
            )
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
            return
        } finally {
            mPdfDocument!!.close()
            mPdfDocument = null
        }
//        val writtenPages: Array<PageRange> = computeWrittenPages()
//        // Signal the print framework the document is complete
//        callback.onWriteFinished(writtenPages)
        callback.onWriteFinished(pageRanges)
    }

    override fun onFinish() {
        super.onFinish()
    }

    // Check to see if this page is in the output range.
    private fun containsPage(p: Array<PageRange>, i: Int): Boolean {
        try {
            val pr = p[i]
        } catch (e: IllegalArgumentException) {
            return false
        }
        return true
    }

    private fun computePageCount(printAttributes: PrintAttributes): Int {
        var itemsPerPage = 4 // default item count for portrait mode
        val pageSize = printAttributes.mediaSize
        if (!pageSize!!.isPortrait) {
            // Six items per page in landscape orientation
            itemsPerPage = 6
        }

        // Determine number of print items
        val printItemCount: Int = 1
        return Math.ceil(printItemCount / itemsPerPage.toDouble()).toInt()
    }

    // 출력물 canvas로 그려야 함.
    private fun drawPage(page: PdfDocument.Page) {
        val canvas: Canvas = page.canvas

        // units are in points (1/72 of an inch)
        val titleBaseLine = 72f
        val leftMargin = 54f
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 36f
        canvas.drawText("Test Title", leftMargin, titleBaseLine, paint)
        paint.textSize = 11f
        canvas.drawText("Test paragraph", leftMargin,
            (titleBaseLine + 25), paint)
        paint.color = Color.BLUE
        canvas.drawRect(100f, 100f, 172f, 172f, paint)
    }
}

