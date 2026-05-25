pub fn parse_ui(content: &str) -> String {
    // Untuk versi super mikro, kita bersihkan whitespace baru 
    // lalu kirim string mentah agar di-parse ringan di Kotlin.
    content
        .lines()
        .map(|line| line.trim())
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .collect::<Vec<_>>()
        .join("\n")
}
