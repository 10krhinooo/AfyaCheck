# Risk-level classification and recommendation text, split out of app.py
# so the FastAPI route layer only wires things together.
from typing import Dict, List


def get_risk_level(score: int) -> str:
    """Determine risk level based on score"""
    if score >= 50:
        return "High"
    elif score >= 25:
        return "Medium"
    else:
        return "Low"


def generate_recommendations(risk_score: int, answers: Dict[str, str]) -> List[str]:
    """Generate recommendations based on risk score and answers"""
    recommendations = []

    # Check if user is sexually active
    is_not_sexually_active = answers.get('sexual_activity', 'no').lower() == 'no'

    if is_not_sexually_active:
        recommendations.extend([
            "Low risk profile: No current sexual activity reported",
            "Consider baseline HIV testing for future reference",
            "Regular health check-ups support overall wellness",
            "Open communication with future partners about sexual health"
        ])
        return recommendations

    # Base recommendations based on risk score (only for sexually active users)
    if risk_score >= 50:
        recommendations.extend([
            "High risk detected: Consider immediate HIV testing and counseling",
            "Consult healthcare provider for comprehensive STI screening",
            "Discuss PrEP (Pre-Exposure Prophylaxis) options with your doctor",
            "Regular testing every 3-6 months strongly recommended"
        ])
    elif risk_score >= 25:
        recommendations.extend([
            "Moderate risk: Schedule HIV testing at your earliest convenience",
            "Consider routine STI screening during next healthcare visit",
            "Practice consistent condom use to reduce transmission risk",
            "Annual HIV testing recommended while sexually active"
        ])
    else:
        recommendations.extend([
            "Low risk: Maintain current protective behaviors",
            "Consider baseline HIV testing for peace of mind",
            "Regular health check-ups support overall wellness",
            "Open communication with partners about sexual health"
        ])

    # Context-specific recommendations based on individual factors
    condom_use = answers.get('condom_use', '').lower()
    if condom_use == 'never':
        recommendations.append("Consistent condom use can significantly reduce HIV transmission risk")

    sexual_partners = answers.get('sexual_partners', '').lower()
    if sexual_partners.isdigit():
        if int(sexual_partners) >= 3:
            recommendations.append("Multiple partners increase risk; consider more frequent testing")
    elif sexual_partners == '3+':
        recommendations.append("Multiple partners increase risk; consider more frequent testing")

    hiv_tested = answers.get('hiv_tested', '').lower()
    if hiv_tested == 'no':
        recommendations.append("Getting tested provides important health information and peace of mind")

    return recommendations
