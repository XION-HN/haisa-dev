#include <jni.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <linux/ptmx.h>

static int ptmxfd = -1;

JNIEXPORT jint JNICALL
Java_com_haisa_terminal_TermExec_createSubprocessInternal(
    JNIEnv *env, jclass clazz,
    jstring cmd, jobjectArray args, jobjectArray envVars, jint masterFd) {

    const char *command = (*env)->GetStringUTFChars(env, cmd, NULL);
    if (command == NULL) {
        return -1;
    }

    jsize nargs = (*env)->GetArrayLength(env, args);
    jsize nenvs = (*env)->GetArrayLength(env, envVars);

    char **argv = (char **)malloc((nargs + 2) * sizeof(char *));
    char **envp = (char **)malloc((nenvs + 1) * sizeof(char *));

    if (!argv || !envp) {
        (*env)->ReleaseStringUTFChars(env, cmd, command);
        free(argv);
        free(envp);
        return -1;
    }

    argv[0] = strdup(command);
    for (jsize i = 0; i < nargs; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char *argStr = (*env)->GetStringUTFChars(env, arg, NULL);
        argv[i + 1] = strdup(argStr);
        (*env)->ReleaseStringUTFChars(env, arg, argStr);
        (*env)->DeleteLocalRef(env, arg);
    }
    argv[nargs + 1] = NULL;

    for (jsize i = 0; i < nenvs; i++) {
        jstring envVar = (jstring)(*env)->GetObjectArrayElement(env, envVars, i);
        const char *envStr = (*env)->GetStringUTFChars(env, envVar, NULL);
        envp[i] = strdup(envStr);
        (*env)->ReleaseStringUTFChars(env, envVar, envStr);
        (*env)->DeleteLocalRef(env, envVar);
    }
    envp[nenvs] = NULL;

    pid_t pid = fork();

    if (pid < 0) {
        (*env)->ReleaseStringUTFChars(env, cmd, command);
        for (int i = 0; argv[i]; i++) free(argv[i]);
        free(argv);
        for (int i = 0; envp[i]; i++) free(envp[i]);
        free(envp);
        return -1;
    }

    if (pid == 0) {
        close(masterFd);

        int ptsfd = open("/dev/ptmx", O_RDWR);
        if (ptsfd < 0) {
            _exit(1);
        }

        if (grantpt(ptsfd) < 0 || unlockpt(ptsfd) < 0) {
            close(ptsfd);
            _exit(1);
        }

        char *slavename = ptsname(ptsfd);
        if (!slavename) {
            close(ptsfd);
            _exit(1);
        }

        pid_t innerPid = fork();
        if (innerPid < 0) {
            _exit(1);
        }

        if (innerPid > 0) {
            close(ptsfd);
            _exit(0);
        }

        int slavefd = open(slavename, O_RDWR);
        if (slavefd < 0) {
            _exit(1);
        }
        close(ptsfd);

        dup2(slavefd, STDIN_FILENO);
        dup2(slavefd, STDOUT_FILENO);
        dup2(slavefd, STDERR_FILENO);
        if (slavefd > 2) close(slavefd);

        setsid();
        ioctl(0, TIOCSCTTY, 0);

        execve(argv[0], argv, envp);
        _exit(127);
    }

    int status;
    waitpid(pid, &status, 0);

    (*env)->ReleaseStringUTFChars(env, cmd, command);
    for (int i = 0; argv[i]; i++) free(argv[i]);
    free(argv);
    for (int i = 0; envp[i]; i++) free(envp[i]);
    free(envp);

    return pid;
}

JNIEXPORT jint JNICALL
Java_com_haisa_terminal_TermExec_waitFor(JNIEnv *env, jclass clazz, jint processId) {
    int status;
    pid_t result = waitpid(processId, &status, 0);
    if (result < 0) {
        return -1;
    }
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    if (WIFSIGNALED(status)) {
        return -WTERMSIG(status);
    }
    return -1;
}

JNIEXPORT void JNICALL
Java_com_haisa_terminal_TermExec_sendSignal(JNIEnv *env, jclass clazz, jint processId, jint signal) {
    kill(processId, signal);
}
