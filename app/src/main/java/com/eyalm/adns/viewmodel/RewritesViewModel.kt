package com.eyalm.adns.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.nextdns.rewrites.Rewrite
import com.eyalm.adns.data.nextdns.rewrites.RewriteError
import com.eyalm.adns.data.nextdns.rewrites.RewriteField
import com.eyalm.adns.data.nextdns.rewrites.RewriteFormValidation
import com.eyalm.adns.domain.nextdns.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewritesUiState(
    val loading: Boolean = false,
    val initialLoadComplete: Boolean = false,
    val items: List<Rewrite> = emptyList(),
    val createDialogOpen: Boolean = false,
    val name: String = "",
    val content: String = "",
    val fieldErrors: Map<RewriteField, RewriteError> = emptyMap(),
    val deleting: Rewrite? = null,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
)

class RewritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ApiRepository(application)

    private val _state = MutableStateFlow(RewritesUiState())
    val state = _state.asStateFlow()

    fun load() {
        if (_state.value.loading) return

        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            when (val result = repository.getRewrites()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        initialLoadComplete = true,
                        items = result.value,
                        errorMessage = null,
                    )
                }
                else -> _state.update {
                    it.copy(
                        loading = false,
                        initialLoadComplete = true,
                        errorMessage = requestFailureMessage(),
                    )
                }
            }
        }
    }

    fun submitCreate() {
        val current = _state.value
        if (current.submitting) return

        val name = current.name.trim()
        val content = current.content.trim()
        val localErrors = RewriteFormValidation.localErrors(name, content)

        if (localErrors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = localErrors) }
            return
        }

        _state.update { it.copy(submitting = true, fieldErrors = emptyMap()) }

        viewModelScope.launch {
            when (val result = repository.createRewrite(name, content)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            submitting = false,
                            createDialogOpen = false,
                            name = "",
                            content = "",
                            fieldErrors = emptyMap(),
                            errorMessage = null,
                        )
                    }
                    load()
                }

                is ApiResult.ServerFailure -> {
                    val errors = RewriteFormValidation.serverErrors(result.problems)
                    _state.update {
                        it.copy(
                            submitting = false,
                            fieldErrors = errors,
                            errorMessage = if (errors.isEmpty()) requestFailureMessage() else null,
                        )
                    }
                }

                else -> _state.update {
                    it.copy(submitting = false, errorMessage = requestFailureMessage())
                }
            }
        }
    }

    fun openCreate() {
        _state.update {
            it.copy(
                createDialogOpen = true,
                fieldErrors = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun dismissCreate() {
        if (_state.value.submitting) return
        _state.update {
            it.copy(
                createDialogOpen = false,
                name = "",
                content = "",
                fieldErrors = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun updateName(value: String) {
        _state.update {
            it.copy(
                name = value,
                fieldErrors = it.fieldErrors - RewriteField.Name,
                errorMessage = null,
            )
        }
    }

    fun updateContent(value: String) {
        _state.update {
            it.copy(
                content = value,
                fieldErrors = it.fieldErrors - RewriteField.Content,
                errorMessage = null,
            )
        }
    }

    fun confirmDelete() {
        val rewrite = _state.value.deleting ?: return
        if (_state.value.submitting) return
        deleteRewrite(rewrite)
    }

    fun dismissDelete() {
        if (_state.value.submitting) return
        _state.update { it.copy(deleting = null) }
    }

    fun requestDelete(rewrite: Rewrite) {
        if (_state.value.submitting) return
        _state.update { it.copy(deleting = rewrite, errorMessage = null) }
    }

    private fun deleteRewrite(rewrite: Rewrite) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            when (repository.deleteRewrite(rewrite.id)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(submitting = false, deleting = null, errorMessage = null)
                    }
                    load()
                }
                else -> {
                    _state.update {
                        it.copy(submitting = false, errorMessage = requestFailureMessage())
                    }
                }
            }
        }
    }
    private fun requestFailureMessage(): String =
        getApplication<Application>().getString(R.string.network_error_please_try_again)
}
