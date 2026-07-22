package com.xunxian.seekingimmortals.quest;

import java.util.List;
import java.util.Set;

/** Bundled FTB pack ownership metadata. This class intentionally has no FTB API dependency. */
final class FtbDefaultPackManifest {
    static final String REVISION = "20260722_2";

    static final List<ManagedFile> FILES = List.of(
            file("data.snbt",
                    "50d890f11368dd3085625ea2b091f827b4a69f2990e5eeb886f933a7dc45f045",
                    "9f710be743127932181dcefa126d5e9844710c80b5dae19fe2feedf8700e418d"),
            file("chapters/seeking_immortals_main.snbt",
                    "2f2ef7f7b23e9b5a19eaa24157bb28231ccd08f5b876ae619066884fc62dae75",
                    "88418ae346a016bcabe14afe4eafa3394b5a30f355a4b310d53bb18b8c1e6799",
                    "b3736fc9c7619db9a9b6200d38985e0802b0833f4f4e24eab4b1b78fd2603f09",
                    "f0377c7d3ec9f685f6e0dbae650107194dec80cdd4a817ba291b56a01d18bf95"),
            file("chapters/seeking_immortals_chaotic_sea.snbt",
                    "c7880011c1d4eaaaba6641d3bab66e6720d9e22769a7a9afa16782af730f615a",
                    "7f6e5f01917ea45025d2fd3fcf930f77cb27b18dd6f1b1105e7d95f82f95d3ba",
                    "dffff62b375ddd8cbcd878088ff3a9122fead8d63d1256e861c0b62987b37a3d"),
            file("chapters/seeking_immortals_dajin_kunwu.snbt",
                    "326565dc1ae1e46bef37e8a4ad4ac7ed9f22c2d21c3d4a594a6b548043858b33",
                    "d80070c85214acb9699cf89c2ba8562e2cbd52a6782e48021bfaf4a37f540aed",
                    "3290c4db7eb86e75ef407263099f81dd22586b3c5ceb596b2fde2311fa110162",
                    "a7c176dbc94a30671839f2025c5a8e89ef4c7116dec1012c127e395ed9961ebe"),
            file("chapters/seeking_immortals_fallen_demon_yin.snbt",
                    "bdb5664a7c55dc799606cf188e289f2aedf6e759682990abd8e65d17afd81f52",
                    "ad5a7b400fbbb661989f3ee06570c536d2ad788875bde786f53e26cff9afd968",
                    "3e8aad6c0abb22fae2d58484b8dea29d125ce36cf2bb6e31bbe019f9c17332c9",
                    "65106ed3c0d292e071ad1eb25a552675e5099495dd5eca95e3886cc3044a5754"),
            file("chapters/seeking_immortals_mulan_demonic.snbt",
                    "0186cad91d4d941528e5e1390b65de33c0d973ae5fc36b81c591a2a006a57625",
                    "9418cb2531d3949f00a212d8af98f6cdbcad391ab5354f31db9845d5e4457d89",
                    "c6c4e14afbe3bbaa7a13b718909a522aa8070098301221154bde19ad492a9f2c",
                    "b11528b8851fc7f0391711cd7d0de6de6ac4f9ac745439daa653d90f689a2797"),
            file("chapters/seeking_immortals_spirit_realm_service.snbt",
                    "79b8249086fa266dac8669ea4d6d0e6f38d0cd625b0a2f9c4c91bc5b97f23c01",
                    "058e32a0f1dfb0f2579a9311c3061d41fca3ba2f6971f36a5137f00d98a26c42",
                    "c037b52dfd9041f0cd3a176a59e38837b5c152d8c689f3438e010bb7a4c93ac9",
                    "e37dbd2123c0d2f2e664284088c8b99f858273aa5523a290b2009916031fab20"),
            file("chapters/seeking_immortals_tiannan_seven_sects.snbt",
                    "d8ab174a2260d27845380ce016334fb85605c06a7202dae1595c688e7d75bac0",
                    "45d57042a10d44a3546d43f26a8684e5bf9d67f24ad2f53758cc4416b1800e35",
                    "f347e715142f73c9cec7a4265540d6297c6f19771135815a86d2ddfeecd3d16c"),
            file("chapters/seeking_immortals_star_palace_inverse.snbt",
                    "b4f0b9a79756d171bb9ca20cc82cf339fd070f18089e875eabbd4cb218c4659b",
                    "808916853afc596b6dd7ec295cb2126e9ba143097e20dcefac080509551a2a18",
                    "da6a205a842c624bb7a2473137b630c448395bd3c46774edfc4f040f5fdff988"),
            file("chapters/seeking_immortals_ascension_border.snbt",
                    "f323425ed4e13ef5948547052969a106df822cfd06875ee5075e2a3032519af4",
                    "d9536f50eb4948afea728f8bdff227eed35fc74361ddb23efe83392583fb11c4",
                    "87d86c05de3031dc7c43673794a8c52a1e222064991117883bd6e8568956971d",
                    "f515d70c0111904a5a626872a08cc694cecfcc940aed5b34ad8ccc9e4e9a52a9")
    );

    private FtbDefaultPackManifest() {}

    private static ManagedFile file(String relativePath, String... historicalHashes) {
        return new ManagedFile(relativePath, Set.of(historicalHashes));
    }

    record ManagedFile(String relativePath, Set<String> historicalHashes) {}
}
