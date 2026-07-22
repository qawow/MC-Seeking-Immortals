package com.xunxian.seekingimmortals.quest;

import java.util.List;
import java.util.Set;

/** Bundled FTB pack ownership metadata. This class intentionally has no FTB API dependency. */
final class FtbDefaultPackManifest {
    static final String REVISION = "20260723_2";

    static final List<ManagedFile> FILES = List.of(
            file("data.snbt",
                    "50d890f11368dd3085625ea2b091f827b4a69f2990e5eeb886f933a7dc45f045",
                    "9f710be743127932181dcefa126d5e9844710c80b5dae19fe2feedf8700e418d"),
            file("chapters/seeking_immortals_main.snbt",
                    "2de8addf0646af2d58d542f15b7c825407c88613ed4ff52aa31046f07c5f397c",
                    "2f2ef7f7b23e9b5a19eaa24157bb28231ccd08f5b876ae619066884fc62dae75",
                    "88418ae346a016bcabe14afe4eafa3394b5a30f355a4b310d53bb18b8c1e6799",
                    "b3736fc9c7619db9a9b6200d38985e0802b0833f4f4e24eab4b1b78fd2603f09",
                    "f0377c7d3ec9f685f6e0dbae650107194dec80cdd4a817ba291b56a01d18bf95",
                    "662d00858970fcdb29ee3d522f6087e3acb1357bc1f8cb7ea69256338b369010",
                    "f423bfe5608970052a963b570c8164b9f5ea677fa0bd073913f0b2dbcc6405b5"),
            file("chapters/seeking_immortals_chaotic_sea.snbt",
                    "d9c5b7394bd5769197ddd43c6e9763f7a5329dd19649a6f05f202b2ae1e0e2e5",
                    "c7880011c1d4eaaaba6641d3bab66e6720d9e22769a7a9afa16782af730f615a",
                    "7f6e5f01917ea45025d2fd3fcf930f77cb27b18dd6f1b1105e7d95f82f95d3ba",
                    "dffff62b375ddd8cbcd878088ff3a9122fead8d63d1256e861c0b62987b37a3d",
                    "080e644a31df460b95408aa290ef795f62877aafe3352744962ce1114f2f8ce2",
                    "21b0ccd3507bf4d763be4cf8cc5cb95da869c89f72b03223f90a42573655aee7"),
            file("chapters/seeking_immortals_dajin_kunwu.snbt",
                    "f65acfe60bef0dbaf99921a311278db9035d2b0dfe5a48042d65d658295d7df9",
                    "326565dc1ae1e46bef37e8a4ad4ac7ed9f22c2d21c3d4a594a6b548043858b33",
                    "d80070c85214acb9699cf89c2ba8562e2cbd52a6782e48021bfaf4a37f540aed",
                    "3290c4db7eb86e75ef407263099f81dd22586b3c5ceb596b2fde2311fa110162",
                    "a7c176dbc94a30671839f2025c5a8e89ef4c7116dec1012c127e395ed9961ebe",
                    "ada753f7daad7e482d93e5a4e1dadbf114dbcf5a08b95039ffc78c2d69bc55f2",
                    "a817c6ca905f8585801a8c230595388477af7ed6818b6215d6b2ed4bcdb6b35c"),
            file("chapters/seeking_immortals_fallen_demon_yin.snbt",
                    "9057225c577d9612f794d3a4af49d5426a57cc420ca532ea6e9e8382a191fcf1",
                    "bdb5664a7c55dc799606cf188e289f2aedf6e759682990abd8e65d17afd81f52",
                    "ad5a7b400fbbb661989f3ee06570c536d2ad788875bde786f53e26cff9afd968",
                    "3e8aad6c0abb22fae2d58484b8dea29d125ce36cf2bb6e31bbe019f9c17332c9",
                    "65106ed3c0d292e071ad1eb25a552675e5099495dd5eca95e3886cc3044a5754",
                    "87b7201e3ac0be4f298eb508c60091ef2107d6494ccddc17306786e99c906515",
                    "3473c4c98c56f54a385d3bf82230d71b96ad3e0b66314cd83e7bff10d41e641f"),
            file("chapters/seeking_immortals_mulan_demonic.snbt",
                    "8ba644cc1ae684513f241f504133068d2c4eb644108fe089bf53dfad8ac050c8",
                    "0186cad91d4d941528e5e1390b65de33c0d973ae5fc36b81c591a2a006a57625",
                    "9418cb2531d3949f00a212d8af98f6cdbcad391ab5354f31db9845d5e4457d89",
                    "c6c4e14afbe3bbaa7a13b718909a522aa8070098301221154bde19ad492a9f2c",
                    "b11528b8851fc7f0391711cd7d0de6de6ac4f9ac745439daa653d90f689a2797",
                    "853fe3eda407c9dd7bf895ece7ad91f178f21fa38f9f1d06cd58578edd306977",
                    "2080a01b3ecff98a84c12ed8c8f2550ab09cd627a5ed47cec97729a0264e8b41"),
            file("chapters/seeking_immortals_spirit_realm_service.snbt",
                    "8fcd07e9461f7acd50ac593f887336dfe23c2d41b5edf61f6f02dbe42b280ebd",
                    "79b8249086fa266dac8669ea4d6d0e6f38d0cd625b0a2f9c4c91bc5b97f23c01",
                    "058e32a0f1dfb0f2579a9311c3061d41fca3ba2f6971f36a5137f00d98a26c42",
                    "c037b52dfd9041f0cd3a176a59e38837b5c152d8c689f3438e010bb7a4c93ac9",
                    "e37dbd2123c0d2f2e664284088c8b99f858273aa5523a290b2009916031fab20",
                    "69388d9d1abdb692cd9f0750607f9657ae5b37beff138b1aae7b9a64f24dbf69",
                    "cc9db8f5623c53586345a15c54e83b2fc2a163e0cfa3c1f7949c3d98721f7329"),
            file("chapters/seeking_immortals_tiannan_seven_sects.snbt",
                    "559244f3e677239668dd7d879374e2ba08e65ed7a3015f529e45b9842bef53a1",
                    "d8ab174a2260d27845380ce016334fb85605c06a7202dae1595c688e7d75bac0",
                    "45d57042a10d44a3546d43f26a8684e5bf9d67f24ad2f53758cc4416b1800e35",
                    "f347e715142f73c9cec7a4265540d6297c6f19771135815a86d2ddfeecd3d16c",
                    "081cb3272548e310cd69e7e8126982a20a858a6275ff71047a27ad3a416f9339",
                    "14307c3517123457772e553b9418a4be1bc71f65f3ce95b9830b056c5b57d897"),
            file("chapters/seeking_immortals_star_palace_inverse.snbt",
                    "12c48b3c11a08381567522b133cc2249abfbd5e641f009c05c384a5237417ec0",
                    "b4f0b9a79756d171bb9ca20cc82cf339fd070f18089e875eabbd4cb218c4659b",
                    "808916853afc596b6dd7ec295cb2126e9ba143097e20dcefac080509551a2a18",
                    "da6a205a842c624bb7a2473137b630c448395bd3c46774edfc4f040f5fdff988",
                    "6548897975d5c605d1e54762279ce82114ff88009cb886a0863e7454bd79d5e0",
                    "51d9f8881d386a6a4ab1d040a7e414dae1543f55adc1f39c201cd46084cad17d"),
            file("chapters/seeking_immortals_ascension_border.snbt",
                    "e5c7e2a9f0f75972103da658f64b071c3e512854a448ecfafc9c8ab500adeb73",
                    "f323425ed4e13ef5948547052969a106df822cfd06875ee5075e2a3032519af4",
                    "d9536f50eb4948afea728f8bdff227eed35fc74361ddb23efe83392583fb11c4",
                    "87d86c05de3031dc7c43673794a8c52a1e222064991117883bd6e8568956971d",
                    "f515d70c0111904a5a626872a08cc694cecfcc940aed5b34ad8ccc9e4e9a52a9",
                    "211fab88670c75ca81cb8cc0edcee51fb4f198dbfdbc47352612cd87a47aaeb4",
                    "9ffee76baa0cc429c1af8e18c65d6b1d9c8d9d0a2e566de7e88fb0ce016d57c7")
    );

    private FtbDefaultPackManifest() {}

    private static ManagedFile file(String relativePath, String... historicalHashes) {
        return new ManagedFile(relativePath, Set.of(historicalHashes));
    }

    record ManagedFile(String relativePath, Set<String> historicalHashes) {}
}
