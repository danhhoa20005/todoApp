package com.example.appmanagement.di

import android.content.Context
import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.remote.TaskRemoteDataSource
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideTaskRemoteDataSource(firestore: FirebaseFirestore): TaskRemoteDataSource =
        TaskRemoteDataSource(firestore)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        userDao: UserDao,
        remoteDataSource: TaskRemoteDataSource
    ): TaskRepository = TaskRepository(taskDao, userDao, remoteDataSource)

    @Provides
    @Singleton
    fun provideAccountRepository(userDao: UserDao): AccountRepository = AccountRepository(userDao)
}
