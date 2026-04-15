package tn.esprit.evaluation.entity;

/**
 * Lot de questions pour parcours différenciés :
 * <ul>
 *   <li>COMMUN — posée dans les deux parcours (standard et renforcement)</li>
 *   <li>STANDARD — uniquement parcours standard</li>
 *   <li>RENFORCEMENT — uniquement parcours renforcement (révision ciblée)</li>
 * </ul>
 */
public enum ParcoursInclusion {
    COMMUN,
    STANDARD,
    RENFORCEMENT
}
