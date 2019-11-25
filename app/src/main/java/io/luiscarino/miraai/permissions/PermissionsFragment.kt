package io.luiscarino.miraai.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import io.luiscarino.miraai.R

private const val PERMISSIONS_REQUEST_CODE = 100
private val APP_PERMISSIONS = arrayOf(
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.CAMERA
)

/**
 * A [Fragment] that handles requesting permissions and, once granted start the application flow.
 */
class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermissions(requireContext())) {
            // if permissions have been granted, navigate
            navigateToCamera()
        } else {
            // Request permissions
            requestPermissions(APP_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()
                navigateToCamera()
            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()

            }
        }
    }

    private fun navigateToCamera() {
        Navigation.findNavController(requireActivity(), R.id.host_fragment).navigate(
            PermissionsFragmentDirections.actionPermissionsToCamera())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }


    companion object {
        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = APP_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
