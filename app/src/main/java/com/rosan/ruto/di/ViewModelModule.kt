package com.rosan.ruto.di

import com.rosan.ruto.ui.viewmodel.AiModelListViewModel
import com.rosan.ruto.ui.viewmodel.ConversationListViewModel
import com.rosan.ruto.ui.viewmodel.ConversationViewModel
import com.rosan.ruto.ui.viewmodel.HomeViewModel
import com.rosan.ruto.ui.viewmodel.MultiTaskPreviewViewModel
import com.rosan.ruto.ui.viewmodel.ScreenListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ScreenListViewModel)
    viewModel { params -> MultiTaskPreviewViewModel(params.get(), get()) }
    viewModelOf(::ConversationListViewModel)
    viewModel { params -> ConversationViewModel(params.get(), get(), get()) }
    viewModelOf(::AiModelListViewModel)
}
