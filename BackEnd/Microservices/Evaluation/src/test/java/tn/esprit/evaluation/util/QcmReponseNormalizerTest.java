package tn.esprit.evaluation.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QcmReponseNormalizerTest {

    @Test
    void extraireLettreReponse_accepteVariantes() {
        assertEquals("A", QcmReponseNormalizer.extraireLettreReponse("a"));
        assertEquals("B", QcmReponseNormalizer.extraireLettreReponse(" b) "));
        assertEquals("C", QcmReponseNormalizer.extraireLettreReponse("(C)"));
        assertEquals("D", QcmReponseNormalizer.extraireLettreReponse("Réponse : D"));
        assertEquals("", QcmReponseNormalizer.extraireLettreReponse(""));
        assertEquals("", QcmReponseNormalizer.extraireLettreReponse("?"));
        assertEquals("", QcmReponseNormalizer.extraireLettreReponse("XYZ"));
    }

    @Test
    void reponseQcmCorrecte_ignoreCasseEtFormat() {
        assertTrue(QcmReponseNormalizer.reponseQcmCorrecte("(a)", "A"));
        assertTrue(QcmReponseNormalizer.reponseQcmCorrecte("  b ", " B "));
        assertFalse(QcmReponseNormalizer.reponseQcmCorrecte("A", "B"));
        assertFalse(QcmReponseNormalizer.reponseQcmCorrecte("", "A"));
        assertFalse(QcmReponseNormalizer.reponseQcmCorrecte("A", ""));
    }

    @Test
    void pourcentagePlancher_sansArrondiFavorable() {
        assertEquals(28, QcmReponseNormalizer.pourcentagePlancher(2, 7));
        assertEquals(57, QcmReponseNormalizer.pourcentagePlancher(4, 7));
        assertEquals(50, QcmReponseNormalizer.pourcentagePlancher(5, 10));
    }
}
