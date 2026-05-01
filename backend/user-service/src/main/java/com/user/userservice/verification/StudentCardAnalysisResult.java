package com.user.userservice.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Output attendu :
 * {
 *   "nom": "",
 *   "prenom": "",
 *   "numero_etudiant": "",
 *   "date_naissance": "",
 *   "valide": true/false,
 *   "corrections": []
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCardAnalysisResult {
    private String nom;
    private String prenom;
    private String numero_etudiant;
    private String date_naissance;
    private String institution;
    private double score;
    private boolean valide;
    private List<String> erreurs = new ArrayList<>();
}
