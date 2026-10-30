package br.com.weslleycampos.simpanel.protocol

enum class MessageType(val code: Byte) {
    Hello(1),
    Frame(2),
    Display(3),
    Error(4),
    Chrome(5),
    Viewport(16),
    Touch(17),
    Scroll(18),
    Key(19),
    Text(20),
    Button(21),
    Settings(22);

    companion object {
        fun of(code: Byte): MessageType? = entries.firstOrNull { it.code == code }
    }
}
