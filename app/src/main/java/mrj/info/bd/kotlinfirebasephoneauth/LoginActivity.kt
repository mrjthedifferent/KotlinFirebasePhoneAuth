package mrj.info.bd.kotlinfirebasephoneauth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import mrj.info.bd.kotlinfirebasephoneauth.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    lateinit var auth: FirebaseAuth
    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.apply {
            otp.visibility = android.view.View.GONE
            resendCode.visibility = android.view.View.GONE
            submit.setOnClickListener() {
                //handling otp field visibility
                if (otp.isVisible) {
                    if (otp.text.isNotEmpty()) {
                        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
                            storedVerificationId, otp.text.toString()
                        )
                        signInWithPhoneAuthCredential(credential)
                        return@setOnClickListener
                    }
                    otp.error = "Please enter OTP"
                    Toast.makeText(this@LoginActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (checkPhone(phone)) {
                    sendVerificationCode("+88${phone.text}")
                }
            }
            resendCode.setOnClickListener {
                if (checkPhone(phone)) {
                    resendVerificationCode("+88${phone.text}", resendToken)
                }
            }
        }

        // Callback function for Phone Auth
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            // This method is called when the verification is completed
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("FirebaseAuth", "onVerificationCompleted Success")
                Toast.makeText(this@LoginActivity, "Verification Success", Toast.LENGTH_SHORT)
                    .show()
                if (!binding.otp.isVisible) {
                    binding.otp.visibility = android.view.View.VISIBLE
                }
                binding.otp.setText(credential.smsCode)
                //** Use it if you want automatic sign in **//
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            }

            // Called when verification is failed add log statement to see the exception
            override fun onVerificationFailed(e: FirebaseException) {
                Log.d("FirebaseAuth", "onVerificationFailed  $e")
                Toast.makeText(this@LoginActivity, "Verification Failed", Toast.LENGTH_SHORT).show()
            }

            // On code is sent by the firebase this method is called
            // in here we start a new activity where user can enter the OTP
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("FirebaseAuth", "onCodeSent: $verificationId")
                Toast.makeText(this@LoginActivity, "Code Sent", Toast.LENGTH_SHORT).show()
                storedVerificationId = verificationId
                resendToken = token
                //visibility of OTP & Resend Code field
                binding.otp.visibility = android.view.View.VISIBLE
                binding.resendCode.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun checkPhone(phone: EditText): Boolean {
        val phn = phone.text.toString()
        if (phn.isEmpty()) {
            phone.error = "Please enter phone number"
            return false
        }
        if (phn.length != 11) {
            phone.error = "Please enter valid phone number"
            return false
        }
        return true
    }

    //resend firebase verification code
    private fun resendVerificationCode(
        number: String,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        Toast.makeText(this@LoginActivity, "Re-sending SMS to $number", Toast.LENGTH_SHORT).show()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .setForceResendingToken(token) // ForceResendingToken
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun sendVerificationCode(number: String) {
        Toast.makeText(this@LoginActivity, "Sending SMS to $number", Toast.LENGTH_SHORT).show()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        Log.d("FirebaseAuth", "Auth started")
    }

    // verifies if the code matches sent by firebase
    // if success start the new activity in our case it is main Activity
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Sign in failed, display a message and update the UI
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}