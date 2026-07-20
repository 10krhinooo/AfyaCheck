// Approximate centroids of Kenya's 47 counties: the search origin fallback when the user
// denies (or the device lacks) geolocation. Precision only needs to be good enough to
// anchor a 10km-radius health-center search near the county's population center.
export interface County {
  name: string
  lat: number
  lng: number
}

export const KENYA_COUNTIES: County[] = [
  { name: 'Baringo', lat: 0.4919, lng: 35.7434 },
  { name: 'Bomet', lat: -0.8015, lng: 35.3027 },
  { name: 'Bungoma', lat: 0.5695, lng: 34.5584 },
  { name: 'Busia', lat: 0.4608, lng: 34.1115 },
  { name: 'Elgeyo-Marakwet', lat: 0.8088, lng: 35.5081 },
  { name: 'Embu', lat: -0.5391, lng: 37.4575 },
  { name: 'Garissa', lat: -0.4536, lng: 39.6461 },
  { name: 'Homa Bay', lat: -0.5273, lng: 34.4571 },
  { name: 'Isiolo', lat: 0.3546, lng: 37.5822 },
  { name: 'Kajiado', lat: -1.8524, lng: 36.7768 },
  { name: 'Kakamega', lat: 0.2827, lng: 34.7519 },
  { name: 'Kericho', lat: -0.3689, lng: 35.2863 },
  { name: 'Kiambu', lat: -1.1714, lng: 36.8356 },
  { name: 'Kilifi', lat: -3.6305, lng: 39.8499 },
  { name: 'Kirinyaga', lat: -0.499, lng: 37.2803 },
  { name: 'Kisii', lat: -0.6817, lng: 34.7666 },
  { name: 'Kisumu', lat: -0.0917, lng: 34.768 },
  { name: 'Kitui', lat: -1.375, lng: 38.0106 },
  { name: 'Kwale', lat: -4.1737, lng: 39.4521 },
  { name: 'Laikipia', lat: 0.3606, lng: 36.7819 },
  { name: 'Lamu', lat: -2.2717, lng: 40.902 },
  { name: 'Machakos', lat: -1.5177, lng: 37.2634 },
  { name: 'Makueni', lat: -1.8039, lng: 37.6244 },
  { name: 'Mandera', lat: 3.9366, lng: 41.867 },
  { name: 'Marsabit', lat: 2.3284, lng: 37.9899 },
  { name: 'Meru', lat: 0.0463, lng: 37.6559 },
  { name: 'Migori', lat: -1.0634, lng: 34.4731 },
  { name: 'Mombasa', lat: -4.0435, lng: 39.6682 },
  { name: "Murang'a", lat: -0.721, lng: 37.1526 },
  { name: 'Nairobi', lat: -1.2921, lng: 36.8219 },
  { name: 'Nakuru', lat: -0.3031, lng: 36.08 },
  { name: 'Nandi', lat: 0.1836, lng: 35.1269 },
  { name: 'Narok', lat: -1.0912, lng: 35.8601 },
  { name: 'Nyamira', lat: -0.5633, lng: 34.9359 },
  { name: 'Nyandarua', lat: -0.1804, lng: 36.5232 },
  { name: 'Nyeri', lat: -0.4197, lng: 36.9476 },
  { name: 'Samburu', lat: 1.2155, lng: 36.954 },
  { name: 'Siaya', lat: 0.0617, lng: 34.2422 },
  { name: 'Taita-Taveta', lat: -3.3161, lng: 38.4849 },
  { name: 'Tana River', lat: -1.6519, lng: 39.6516 },
  { name: 'Tharaka-Nithi', lat: -0.2965, lng: 37.7238 },
  { name: 'Trans Nzoia', lat: 1.0567, lng: 34.9507 },
  { name: 'Turkana', lat: 3.1122, lng: 35.5966 },
  { name: 'Uasin Gishu', lat: 0.5143, lng: 35.2698 },
  { name: 'Vihiga', lat: 0.0771, lng: 34.7223 },
  { name: 'Wajir', lat: 1.7471, lng: 40.0573 },
  { name: 'West Pokot', lat: 1.621, lng: 35.3905 },
]
