package me.eungi.criminalintent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val DIALOG_PICTURE = "DialogPicture"
private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 1
private const val REQUEST_CONTACT = 2
private const val REQUEST_PHOTO = 3
private const val DATE_FORMAT = "yyyy-MM-dd"
private const val TIME_FORMAT = "HH:mm"
private const val DATE_FORMAT_REPORT = "yyyy년 M월 d일 H시 m분, E요일"

class CrimeFragment: Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var callButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
    // or a lateinit var in your onAttach() or onCreate() method.
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                suspectButton.callOnClick()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Snackbar.make(suspectButton.rootView, "asdfas", Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        Log.d(TAG, "args bundle crime ID: $crimeId")
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        timeButton = view.findViewById(R.id.crime_time) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        callButton = view.findViewById(R.id.crime_call) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        photoView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (photoView.contentDescription == getString(R.string.crime_photo_no_image_description))
                    updatePhotoView()
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner, Observer { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "me.eungi.criminalintent.fileprovider",
                    photoFile
                )
                updateUI()
                updatePhotoView()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked -> crime.isSolved = isChecked }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        callButton.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${crime.phone}"))
            startActivity(callIntent)
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                } else {
                    startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                }
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                Log.d(TAG, "Disable suspectButton, can't found ACTION_PICK resolve activity")
//                isEnabled = false
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                Log.d(TAG, "Disable photoButton, can't found ACTION_IMAGE_CAPTURE resolve activity")
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(cameraActivity.activityInfo.packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }

        photoView.setOnClickListener {
            if (photoFile.exists()) {
                CrimePictureFragment.newInstance(photoFile.path).apply {
                    show(this@CrimeFragment.parentFragmentManager, DIALOG_PICTURE)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri = data.data ?: return
                // 쿼리에서 값으로 반환할 필드를 지정한다
                val queryFields = arrayOf(ContactsContract.Contacts._ID)
                Log.d(TAG, "queryFields ${queryFields.joinToString(",")}")
                // 쿼리를 수행한다
                val cursor = requireActivity().contentResolver.query(contactUri, queryFields, null, null, null)
                cursor?.use {
                    // 쿼리 결과 데이터가 있는지 확인한다
                    if (it.count == 0 || it.columnCount == 0) {
                        Log.d(TAG, "Row or column cnt 0")
                        return
                    }
                    // 첫 번째 데이터 행의 첫 번째 열의 값을 가져온다
                    // 이 값이 Contacts._ID
                    it.moveToFirst()
                    val id = it.getString(0)
                    Log.d(TAG, "ID $id")
                    val cursor2 = requireActivity().contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                        null,
                        null
                    )
                    cursor2?.use {
                        // 쿼리 결과 데이터가 있는지 확인한다
                        if (it.count == 0 || it.columnCount == 0) {
                            Log.d(TAG, "Row or column cnt 0")
                            return
                        }
                        it.moveToFirst()
                        val displayName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        val hasPhoneNumber = it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                        val phoneNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        Log.d(TAG, "$displayName, $hasPhoneNumber, $phoneNumber")

                        crime.suspect = displayName
                        if (hasPhoneNumber == 1) {
                            crime.phone = phoneNumber
                            callButton.isEnabled = true
                        }
                        crimeDetailViewModel.saveCrime(crime)
                        suspectButton.text = displayName
                    }


                }
            }
            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }


    private fun updateUI() {
        val dateFormatter =  SimpleDateFormat(DATE_FORMAT, Locale.KOREA)
        val timeFormatter =  SimpleDateFormat(TIME_FORMAT, Locale.KOREA)
        titleField.setText(crime.title)
        dateButton.text = dateFormatter.format(crime.date)
        timeButton.text = timeFormatter.format(crime.date)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        if (crime.phone.isNotEmpty()) {
            callButton.isEnabled = true
        }
    }

    private fun updatePhotoView() {
        if (photoFile.exists() && photoView.height != 0) {
            val bitmap = getScaledBitmap(photoFile.path, photoView.height, photoView.width)
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription = getString(R.string.crime_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT_REPORT, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }

    override fun onDateSelected(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = crime.date
        val preHour = calendar.get(Calendar.HOUR_OF_DAY)
        val preMinute = calendar.get(Calendar.MINUTE)
        val preSecond = calendar.get(Calendar.SECOND)
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, preHour)
        calendar.set(Calendar.MINUTE, preMinute)
        calendar.set(Calendar.SECOND, preSecond)
        crime.date = calendar.time
        updateUI()
    }

    override fun onTimeSelected(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = crime.date
        val preYear = calendar.get(Calendar.YEAR)
        val preMonth = calendar.get(Calendar.MONTH)
        val preDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.time = date
        calendar.set(Calendar.YEAR, preYear)
        calendar.set(Calendar.MONTH, preMonth)
        calendar.set(Calendar.DAY_OF_MONTH, preDayOfMonth)
        crime.date = calendar.time
        updateUI()
    }
}

/*
설치된 패키지를 확인하는 resolve activity 의 경우 null 이 나오는 경우가 있음
https://developer.android.com/training/package-visibility/declaring
https://forums.bignerdranch.com/t/resolveactivity-return-null-but-startactivity-works-if-not-checking/18143/2
 */