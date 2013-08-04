#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#include <android/log.h>

#define  LOG_TAG "hostage: p"
#define  LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define CONTROLLEN CMSG_LEN(sizeof(int))

char *socket_path = "\0hostage";

int ipc_sock() {
	int fd;
	struct sockaddr_un addr;

	if ((fd = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
		LOGE("Unable to create local socket: %d", errno);
		return -1;
	}

	memset(&addr, 0, sizeof(addr));
	addr.sun_family = AF_UNIX;
	strncpy(addr.sun_path, socket_path, sizeof(addr.sun_path) - 1);

	if (connect(fd, (struct sockaddr*) &addr, sizeof(addr)) == -1) {
		LOGE("Unable to connect local socket: %d", errno);
		return -1;
	}

	return fd;
}

int net_sock(int port) {
	int fd;
	struct sockaddr_in addr;

	if ((fd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
		LOGE("Unable to create net socket: %d", errno);
		return -1;
	}

	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = INADDR_ANY;
	addr.sin_port = htons(port);

	if (bind(fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
		LOGE("Unable to bind net socket: %d", errno);
		return -1;
	}

	if (listen(fd, 5) == -1) {
		LOGE("Unable to listen net socket: %d", errno);
		return -1;
	}

	return fd;
}

int send_fd(int fd, int fd_to_send) {
	struct iovec iov[1];
	struct cmsghdr *cmptr;
	struct msghdr msg;
	char buf[2] = "FD";

	iov[0].iov_base = buf;
	iov[0].iov_len = 2;

	cmptr = malloc(CONTROLLEN);
	cmptr->cmsg_level = SOL_SOCKET;
	cmptr->cmsg_type = SCM_RIGHTS;
	cmptr->cmsg_len = CONTROLLEN;

	msg.msg_iov = iov;
	msg.msg_iovlen = 1;
	msg.msg_name = NULL;
	msg.msg_namelen = 0;
	msg.msg_control = cmptr;
	msg.msg_controllen = CONTROLLEN;
	*(int *) CMSG_DATA(cmptr) = fd_to_send;

	if (sendmsg(fd, &msg, 0) == -1) {
		LOGE("sendmsg failed: %d", errno);
	}

	return 0;
}

int main(int argc, char *argv[]) {
	int port;
	int ipc_fd, net_fd;

	if (argc < 2) {
		exit(EXIT_FAILURE);
	}

	if ((port = atoi(argv[1])) < 1 || (port = atoi(argv[1])) > 65535) {
		exit(EXIT_FAILURE);
	}

	if ((ipc_fd = ipc_sock()) == -1) {
		close(ipc_fd);
		exit(EXIT_FAILURE);
	}
	LOGI("ipc_fd: %d", ipc_fd);

	if ((net_fd = net_sock(port)) == -1) {
		close(ipc_fd);
		close(net_fd);
		exit(EXIT_FAILURE);
	}
	LOGI("net_fd: %d", net_fd);

	int status;
	status = send_fd(ipc_fd, net_fd);
	LOGI("send_fd: %d", status);

	close(ipc_fd);
	close(net_fd);

	if (status == -1) {
		return (EXIT_FAILURE);
	}

	return EXIT_SUCCESS;
}
