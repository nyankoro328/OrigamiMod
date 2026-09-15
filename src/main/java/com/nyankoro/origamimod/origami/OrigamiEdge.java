package com.nyankoro.origamimod.origami;

/*
 * 折り上がり図で実際に表示する境界線。
 *
 * SubFaceの全境界ではなく、
 * Oriedita本家と同じ条件で選別された線のみ保持する。
 */
public record OrigamiEdge(
        OrigamiVertex a,
        OrigamiVertex b
) {
}