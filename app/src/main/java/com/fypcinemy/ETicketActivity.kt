package com.fypcinemy

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.fypcinemy.utils.QrCodeGenerator
import java.io.OutputStream

class ETicketActivity : BaseActivity() {

    private companion object {
        const val TAG = "ETicketActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_e_ticket)

        val textMovie = findViewById<TextView>(R.id.textETicketMovie)
        val textSeats = findViewById<TextView>(R.id.textETicketSeats)
        val textShowtime = findViewById<TextView>(R.id.textETicketShowtime)
        val textTotal = findViewById<TextView>(R.id.textETicketTotal)
        val textBookingId = findViewById<TextView>(R.id.textBookingId)
        val imgQrCode = findViewById<ImageView>(R.id.imgQrCode)
        val ticketView = findViewById<LinearLayout>(R.id.ticketView)

        val btnDownload = findViewById<Button>(R.id.btnDownloadTicket)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        val movieTitle = intent.getStringExtra("movieTitle")
        val showtime = intent.getStringExtra("showtime")

        val selectedSeats =
            intent.getStringArrayListExtra("selectedSeats")

        val totalPrice =
            intent.getIntExtra("totalPrice", 0)

        textMovie.text = movieTitle

        textShowtime.text = if (showtime.isNullOrBlank()) "" else "Time: $showtime"
        textShowtime.visibility = if (showtime.isNullOrBlank()) View.GONE else View.VISIBLE

        textSeats.text =
            getString(R.string.seats_label, selectedSeats?.joinToString(", ") ?: "")

        textTotal.text = getString(R.string.paid_label, totalPrice)

        val bookingId = intent.getStringExtra("bookingId").orEmpty()

        textBookingId.text = getString(R.string.booking_id_label, bookingId)

        val qrBitmap = QrCodeGenerator.generateQrCode(bookingId, 500, 500)
        imgQrCode.setImageBitmap(qrBitmap)

        setupBottomNavigation()

        btnDownload.setOnClickListener {
            saveTicketAsPdf(ticketView, "CineMY_Ticket_$bookingId")
        }

        btnBackHome.setOnClickListener {

            val intent =
                Intent(this, HomeActivity::class.java)

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)

            finish()
        }
    }

    private fun saveTicketAsPdf(view: View, fileName: String) {
        if (view.width <= 0 || view.height <= 0) {
            Toast.makeText(this, "Ticket view not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(view.width, view.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri: Uri? = resolver.insert(collection, contentValues)

        if (uri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    
                    Log.d(TAG, "Ticket saved successfully: $uri")
                    Toast.makeText(this, "Ticket saved to Downloads folder", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save ticket", e)
                Toast.makeText(this, "Failed to save ticket: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(TAG, "Failed to create MediaStore entry")
            Toast.makeText(this, "Failed to create file entry", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection()
    }
}
