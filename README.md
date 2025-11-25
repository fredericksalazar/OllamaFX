# OllamaFX 🦙✨

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Beta-yellow?style=for-the-badge)

**OllamaFX** is a modern, native desktop client for [Ollama](https://ollama.com/), built with JavaFX. It provides a beautiful, user-friendly interface to manage your local LLMs and chat with them, featuring a sleek GNOME/Adwaita-inspired design.

## ✨ Features

*   **🎨 Modern UI:** Clean, responsive interface inspired by GNOME Adwaita, with full **Light/Dark mode** support.
*   **📦 Model Management:**
    *   **Install:** Browse the Ollama library and download models with a real-time progress popup.
    *   **Manage:** View installed models, check details (size, format), and uninstall them easily.
    *   **Instant Feedback:** The UI updates instantly upon installation or deletion.
*   **💬 Chat Interface:**
    *   Create multiple chat sessions.
    *   **Pin**, **Rename**, and **Delete** chats.
    *   Clean message bubbles with distinct styles for user and AI.
*   **🛠️ Tech Stack:** Built on the robust JavaFX platform, utilizing `ollama4j` for API interaction and `AtlantaFX` for theming.

## 🚀 Getting Started

### Prerequisites

*   **Java 17** or higher installed.
*   **Ollama** installed and running locally (`ollama serve`).

### Installation & Running

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/OllamaFX.git
    cd OllamaFX
    ```

2.  **Run the application:**
    ```bash
    ./gradlew run
    ```

3.  **Build a native image (Optional):**
    ```bash
    ./gradlew jlink
    ```

## 🤝 Contributing

Contributions are welcome! Please follow our standard flow:

1.  Fork the project.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with ❤️ by the OllamaFX Team.*
