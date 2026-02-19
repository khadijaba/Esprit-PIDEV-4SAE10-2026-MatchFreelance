package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.dto.CertificatDto;
import tn.esprit.evaluation.entity.Certificat;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.repository.CertificatRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificatService {

    private final CertificatRepository certificatRepository;

    /**
     * Crée un certificat si le passage est réussi. Idempotent : ne crée pas de doublon.
     */
    @Transactional
    public CertificatDto creerSiReussi(PassageExamen passage) {
        if (passage.getResultat() != PassageExamen.ResultatExamen.REUSSI)
            return null;
        if (certificatRepository.existsByPassageExamenId(passage.getId()))
            return certificatRepository.findByPassageExamenId(passage.getId())
                    .map(CertificatDto::fromEntity)
                    .orElse(null);

        Certificat cert = Certificat.builder()
                .numeroCertificat(Certificat.genererNumero())
                .passageExamen(passage)
                .build();
        cert = certificatRepository.save(cert);
        return CertificatDto.fromEntity(cert);
    }

    @Transactional(readOnly = true)
    public List<CertificatDto> findByFreelancer(Long freelancerId) {
        return certificatRepository.findByPassageExamen_FreelancerIdOrderByDateDelivranceDesc(freelancerId).stream()
                .map(CertificatDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CertificatDto findById(Long id) {
        return certificatRepository.findById(id)
                .map(CertificatDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Certificat non trouvé: " + id));
    }

    @Transactional(readOnly = true)
    public CertificatDto findByPassageExamen(Long passageExamenId) {
        return certificatRepository.findByPassageExamenId(passageExamenId)
                .map(CertificatDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Aucun certificat pour ce passage: " + passageExamenId));
    }
}
