import Foundation

public enum SocketError: Error, CustomStringConvertible {
    case pathTooLong(String)
    case system(String, Int32)

    public var description: String {
        switch self {
        case .pathTooLong(let path): "socket path longer than 103 bytes: \(path)"
        case .system(let call, let code): "\(call) failed: \(String(cString: strerror(code)))"
        }
    }
}

/// One end of a Unix domain socket. Writes from several threads are serialized.
public final class Connection: @unchecked Sendable {
    private let fd: Int32
    private let writeLock = NSLock()

    init(fd: Int32) {
        self.fd = fd
        var on: Int32 = 1
        setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, &on, socklen_t(MemoryLayout<Int32>.size))
    }

    deinit {
        Darwin.close(fd)
    }

    public static func listen(at path: String) throws -> Int32 {
        var address = try socketAddress(path)
        unlink(path)
        let fd = socket(AF_UNIX, SOCK_STREAM, 0)
        guard fd >= 0 else { throw SocketError.system("socket", errno) }
        let bound = withUnsafePointer(to: &address) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                bind(fd, $0, socklen_t(MemoryLayout<sockaddr_un>.size))
            }
        }
        guard bound == 0 else { throw SocketError.system("bind", errno) }
        chmod(path, 0o600)
        guard Darwin.listen(fd, 1) == 0 else { throw SocketError.system("listen", errno) }
        return fd
    }

    public static func accept(_ listener: Int32) throws -> Connection {
        while true {
            let fd = Darwin.accept(listener, nil, nil)
            if fd >= 0 { return Connection(fd: fd) }
            if errno != EINTR { throw SocketError.system("accept", errno) }
        }
    }

    public static func connect(to path: String) throws -> Connection {
        var address = try socketAddress(path)
        let fd = socket(AF_UNIX, SOCK_STREAM, 0)
        guard fd >= 0 else { throw SocketError.system("socket", errno) }
        let connected = withUnsafePointer(to: &address) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                Darwin.connect(fd, $0, socklen_t(MemoryLayout<sockaddr_un>.size))
            }
        }
        guard connected == 0 else {
            let code = errno
            Darwin.close(fd)
            throw SocketError.system("connect", code)
        }
        return Connection(fd: fd)
    }

    public func write(_ data: Data) throws {
        try writeLock.withLock {
            try data.withUnsafeBytes { (bytes: UnsafeRawBufferPointer) in
                guard let base = bytes.baseAddress else { return }
                var offset = 0
                while offset < bytes.count {
                    let written = Darwin.write(fd, base + offset, bytes.count - offset)
                    if written < 0 {
                        if errno == EINTR { continue }
                        throw SocketError.system("write", errno)
                    }
                    offset += written
                }
            }
        }
    }

    /// Blocks until bytes arrive; nil once the other end has closed.
    public func read(maxLength: Int = 64 * 1024) throws -> Data? {
        var buffer = [UInt8](repeating: 0, count: maxLength)
        while true {
            let count = Darwin.read(fd, &buffer, maxLength)
            if count > 0 { return Data(buffer[0..<count]) }
            if count == 0 { return nil }
            if errno != EINTR { throw SocketError.system("read", errno) }
        }
    }

    /// Whether a read would return without blocking, waiting up to `timeout` seconds.
    public func waitForData(timeout: Double) -> Bool {
        var descriptor = pollfd(fd: fd, events: Int16(POLLIN), revents: 0)
        return poll(&descriptor, 1, Int32(timeout * 1000)) > 0
    }

    private static func socketAddress(_ path: String) throws -> sockaddr_un {
        var address = sockaddr_un()
        let bytes = Array(path.utf8)
        guard bytes.count < MemoryLayout.size(ofValue: address.sun_path) else {
            throw SocketError.pathTooLong(path)
        }
        address.sun_len = UInt8(MemoryLayout<sockaddr_un>.size)
        address.sun_family = sa_family_t(AF_UNIX)
        withUnsafeMutableBytes(of: &address.sun_path) { raw in
            raw.copyBytes(from: bytes)
        }
        return address
    }
}
