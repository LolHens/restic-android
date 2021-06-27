package de.lolhens.resticui.ui.repo

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.*
import de.lolhens.resticui.databinding.FragmentRepoEditBinding
import java.net.URI

class RepoEditFragment : Fragment() {
    private var _binding: FragmentRepoEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var _repoId: RepoConfigId
    private val repoId: RepoConfigId get() = _repoId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRepoEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _repoId = (requireActivity() as RepoActivity).repoId
        val repo = MainActivity.instance.config.repos.find { it.base.id == repoId }

        if (repo != null) {
            binding.editRepoName.setText(repo.base.name)
            binding.editRepoPassword.setText(repo.base.password)
            val s3RepoParams = repo.params as S3RepoParams
            binding.editS3Url.setText(s3RepoParams.s3Url.toString())
            binding.editAccessKeyId.setText(s3RepoParams.accessKeyId)
            binding.editSecretAccessKey.setText(s3RepoParams.secretAccessKey)
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            R.id.action_done -> {
                val repoName = binding.editRepoName.text.toString()
                val repoPassword = binding.editRepoPassword.text.toString()
                val s3UrlString = binding.editS3Url.text.toString()
                val accessKeyId = binding.editAccessKeyId.text.toString()
                val secretAccessKey = binding.editSecretAccessKey.text.toString()

                if (
                    repoName.length > 0 &&
                    s3UrlString.length > 0
                ) {
                    val repo = RepoConfig(
                        RepoBaseConfig(
                            repoId,
                            repoName,
                            RepoType.S3,
                            repoPassword
                        ),
                        S3RepoParams(
                            URI(s3UrlString),
                            accessKeyId,
                            secretAccessKey
                        )
                    )

                    fun saveRepo() {
                        MainActivity.instance.configure { config ->
                            config.copy(repos = config.repos.filterNot { it.base.id == repoId }
                                .plus(repo))
                        }

                        RepoActivity.start(this, false, repoId)

                        requireActivity().finish()
                    }

                    Toast.makeText(context, R.string.text_saving, Toast.LENGTH_SHORT).show()

                    val resticRepo = repo.repo(MainActivity.instance.restic)
                    resticRepo.stats().handle { _, throwable ->
                        if (throwable == null) {
                            saveRepo()
                        } else {
                            throwable.printStackTrace()
                            requireActivity().runOnUiThread {
                                AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.alert_init_repo_title)
                                    .setMessage(R.string.alert_init_repo_message)
                                    .setPositiveButton(android.R.string.ok) { dialog, buttonId ->
                                        resticRepo.init().handle { _, throwable ->
                                            if (throwable == null) {
                                                saveRepo()
                                            } else {
                                                requireActivity().runOnUiThread {
                                                    AlertDialog.Builder(requireActivity())
                                                        .setTitle(R.string.alert_save_repo_title)
                                                        .setMessage(R.string.alert_save_repo_message)
                                                        .setPositiveButton(android.R.string.ok) { dialog, buttonId ->
                                                            saveRepo()
                                                        }
                                                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                                        .show()
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                    .show()
                            }
                        }
                    }

                    true
                } else {
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}