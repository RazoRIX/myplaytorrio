package com.playtorrio.tv.core.tmdb

data class NetworkDirectoryItem(
    val id: Int,
    val name: String,
    val kind: String, // "network" or "company"
    val logoUrl: String? = null,
    val aliases: List<String> = emptyList()
)

object NetworkDirectory {
    val items: List<NetworkDirectoryItem> = listOf(
        // Streaming Networks
        NetworkDirectoryItem(
            id = 213,
            name = "Netflix",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
            aliases = listOf("netflix", "net flix", "netflicks")
        ),
        NetworkDirectoryItem(
            id = 2739,
            name = "Disney+",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/uzKjVDmQ1WRMvGBb7UUX0TaW5TC.png",
            aliases = listOf("disney", "disney+", "disney plus", "disneyplus")
        ),
        NetworkDirectoryItem(
            id = 49,
            name = "HBO",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/tuomPhY2UtuPTqqFnKMVHvSb724.png",
            aliases = listOf("hbo", "home box office")
        ),
        NetworkDirectoryItem(
            id = 3186,
            name = "Max",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/nmU0UMDJB3JwRRQkyvfWTZyNgJw.png",
            aliases = listOf("max", "hbo max", "hbomax")
        ),
        NetworkDirectoryItem(
            id = 2552,
            name = "Apple TV+",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/4KAy34EHvGs2m00axMVnUsEcRsj.png",
            aliases = listOf("apple", "apple tv", "apple tv+", "appletv", "apple tv plus")
        ),
        NetworkDirectoryItem(
            id = 1024,
            name = "Amazon Prime Video",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/ifhbNuuqnlwYqXZoNAVvlTu48Da.png",
            aliases = listOf("amazon", "prime", "prime video", "amazon prime", "primevideo")
        ),
        NetworkDirectoryItem(
            id = 453,
            name = "Hulu",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/pqUTCleNUiTLAVljeoTAfqVN2aH.png",
            aliases = listOf("hulu")
        ),
        NetworkDirectoryItem(
            id = 4330,
            name = "Paramount+",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/fi83B1oztoS47xxcemFdPMhIzK.png",
            aliases = listOf("paramount", "paramount+", "paramount plus", "paramountplus")
        ),
        NetworkDirectoryItem(
            id = 3353,
            name = "Peacock",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/8GJjw3HH6Vn71WqPxs4pMgU0w31.png",
            aliases = listOf("peacock", "peacock tv", "peacocktv")
        ),
        NetworkDirectoryItem(
            id = 1112,
            name = "Crunchyroll",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/mDEb0Y4zZgV8d0rM3n3Z7mJ1p1.png",
            aliases = listOf("crunchyroll", "crunchy roll")
        ),
        NetworkDirectoryItem(
            id = 4,
            name = "BBC",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/mF2pZ2z8J7Vz1m8J5M6Y6e9x5n.png",
            aliases = listOf("bbc", "bbc one", "bbc two", "bbc tv")
        ),
        NetworkDirectoryItem(
            id = 67,
            name = "Showtime",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/Allse9kz5lvTw0iIrMEVoGQ8StX.png",
            aliases = listOf("showtime")
        ),
        NetworkDirectoryItem(
            id = 174,
            name = "AMC",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/pmvRmAToc9Up262W1z9893gWfsm.png",
            aliases = listOf("amc", "amc+")
        ),
        NetworkDirectoryItem(
            id = 88,
            name = "FX",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/aeyVLQ6aj7Fz0P87p4e3N6P9N6A.png",
            aliases = listOf("fx", "fx networks")
        ),
        NetworkDirectoryItem(
            id = 318,
            name = "Starz",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/8GJjw3HH6Vn71WqPxs4pMgU0w31.png",
            aliases = listOf("starz")
        ),
        NetworkDirectoryItem(
            id = 80,
            name = "Adult Swim",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/9AKGkh8OPR225ne9ndvflz5p0j4.png",
            aliases = listOf("adult swim", "adultswim")
        ),
        NetworkDirectoryItem(
            id = 56,
            name = "Cartoon Network",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/c5OC6o8OF6LI89n62Z9wE7mB2k.png",
            aliases = listOf("cartoon network", "cartoonnetwork", "cn")
        ),
        NetworkDirectoryItem(
            id = 13,
            name = "Nickelodeon",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/ikZXxg6KTvGk0q5jM3Xz0V6F7u0.png",
            aliases = listOf("nickelodeon", "nick")
        ),
        NetworkDirectoryItem(
            id = 71,
            name = "The CW",
            kind = "network",
            logoUrl = "https://image.tmdb.org/t/p/w300/ge9hzeaU7nMtQ4PjkFlvdpl99Dr.png",
            aliases = listOf("the cw", "thecw", "cw")
        ),

        // Movie Studios / Production Companies
        NetworkDirectoryItem(
            id = 420,
            name = "Marvel Studios",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/hUzeosd33nzE5MCNsZxCGEKTXaQ.png",
            aliases = listOf("marvel", "marvel studios", "mcu")
        ),
        NetworkDirectoryItem(
            id = 174,
            name = "Warner Bros. Pictures",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/IuAlhZ9ba9umwh2PpIezv0TnAA.png",
            aliases = listOf("warner", "warner bros", "warner brothers", "wb", "warner pictures")
        ),
        NetworkDirectoryItem(
            id = 2,
            name = "Walt Disney Pictures",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/wdrCwmR5Yv1d39559Q08r94vXg7.png",
            aliases = listOf("walt disney", "disney pictures", "disney studios")
        ),
        NetworkDirectoryItem(
            id = 33,
            name = "Universal Pictures",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/8lvHyhjr8oUKOOy2dKXoALWKdp0.png",
            aliases = listOf("universal", "universal pictures", "universal studios")
        ),
        NetworkDirectoryItem(
            id = 5,
            name = "Columbia Pictures",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/71BqEFAF4V3qjjMPCpLuyVFB9A.png",
            aliases = listOf("columbia", "columbia pictures", "sony pictures", "sony")
        ),
        NetworkDirectoryItem(
            id = 4,
            name = "Paramount Pictures",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/fycMZt242LVjagMByZOLUGbCvv3.png",
            aliases = listOf("paramount pictures", "paramount studios")
        ),
        NetworkDirectoryItem(
            id = 25,
            name = "20th Century Studios",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/qZCc1lty5FzX32PlQQzLqiEjQG3.png",
            aliases = listOf("20th century", "20th century fox", "fox", "20th century studios")
        ),
        NetworkDirectoryItem(
            id = 41077,
            name = "A24",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/1ZXsGaFPQJv01WufB29YRiip3A.png",
            aliases = listOf("a24", "a 24")
        ),
        NetworkDirectoryItem(
            id = 3,
            name = "Pixar",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/1TjvG00z616axMRLqZ15727bTe1.png",
            aliases = listOf("pixar", "pixar animation")
        ),
        NetworkDirectoryItem(
            id = 521,
            name = "DreamWorks Animation",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/kP79ug2JpcZGeSpax3htSqnxVGZ.png",
            aliases = listOf("dreamworks", "dream works", "dreamworks animation")
        ),
        NetworkDirectoryItem(
            id = 1,
            name = "Lucasfilm",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/o86DbpburjxrqAzEDhXZcyE8pDb.png",
            aliases = listOf("lucasfilm", "lucas film", "star wars")
        ),
        NetworkDirectoryItem(
            id = 10342,
            name = "Studio Ghibli",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/77AsFHK3m9uvhI5v5uCgN5Z5G4v.png",
            aliases = listOf("ghibli", "studio ghibli")
        ),
        NetworkDirectoryItem(
            id = 1632,
            name = "Lionsgate",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/pA2k9Yv3G8q6jK2N8e1I6s8B7H.png",
            aliases = listOf("lionsgate", "lions gate")
        ),
        NetworkDirectoryItem(
            id = 3172,
            name = "Blumhouse Productions",
            kind = "company",
            logoUrl = "https://image.tmdb.org/t/p/w300/k51nnd5qT7fPvi27K77cI8VwYn1.png",
            aliases = listOf("blumhouse", "blumhouse productions")
        )
    )

    fun search(query: String): List<NetworkDirectoryItem> {
        val q = query.trim().lowercase().replace(Regex("""[^a-z0-9\s]"""), "")
        if (q.length < 2) return emptyList()

        return items.filter { item ->
            val cleanName = item.name.lowercase().replace(Regex("""[^a-z0-9\s]"""), "")
            if (cleanName.contains(q) || q.contains(cleanName)) return@filter true

            item.aliases.any { alias ->
                val cleanAlias = alias.lowercase().replace(Regex("""[^a-z0-9\s]"""), "")
                cleanAlias.contains(q) || q.contains(cleanAlias)
            }
        }
    }
}
