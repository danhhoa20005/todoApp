package com.example.appmanagement.ui.onboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.viewmodel.SignInViewModel
import com.example.appmanagement.databinding.FragmentOnboardBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class OnboardFragment : Fragment() {

    private var _binding: FragmentOnboardBinding? = null
    private val binding get() = _binding!!

    // ViewModel lưu user vào Room
    private val viewModel: SignInViewModel by viewModels()

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    // launcher nhận kết quả Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                toast("Không lấy được idToken!")
                android.util.Log.e("Onboard", "idToken == null")
            }

        } catch (e: ApiException) {
            // LỖI NẰM Ở ĐÂY
            android.util.Log.e("Onboard", "GoogleSignIn ApiException code=${e.statusCode}", e)
            toast("Đăng nhập Google thất bại: code=${e.statusCode}")
        } catch (e: Exception) {
            android.util.Log.e("Onboard", "GoogleSignIn Exception", e)
            toast("Đăng nhập Google thất bại (Exception)!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // cấu hình Google Sign-In (dùng default_web_client_id sinh từ google-services.json)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // tiếp tục bằng email
        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_onboard_to_signEmail)
        }

        // tiếp tục bằng Google
        binding.btgmail.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(intent)
        }
    }

    // dùng idToken để đăng nhập FirebaseAuth, sau đó lưu user vào Room
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    toast("Không lấy được FirebaseUser!")
                    return@addOnSuccessListener
                }

                viewModel.loginWithGoogleUser(
                    firebaseUser = firebaseUser,
                    onSuccess = { user ->
                        toast("Xin chào ${user.name}!")
                        findNavController().navigate(R.id.homeFragment)
                    },
                    onError = {
                        toast("Lỗi lưu tài khoản vào hệ thống!")
                    }
                )
            }
            .addOnFailureListener {
                toast("Firebase xác thực thất bại!")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
