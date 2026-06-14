#!/usr/bin/env bash

set -Eeuo pipefail

readonly DATASET_URL="https://www.donneesquebec.ca/recherche/dataset/879abf6e-c6b2-430a-b44a-16335467c6f6/resource/9555031e-cfc5-4b78-bec9-4ab84b549f67/download/vdq-permis.csv"
readonly EXPECTED_HEADER="NUMERO_PERMIS,DATE_DELIVRANCE,ADRESSE_TRAVAUX,DOMAINE,LOTS_IMPACTES,TYPE_PERMIS,ARRONDISSEMENT,RAISON,LONGITUDE,LATITUDE"

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
readonly OUTPUT_DIR="${PROJECT_ROOT}/data/raw"
readonly OUTPUT_FILE="${OUTPUT_DIR}/permits.csv"

if ! command -v curl >/dev/null 2>&1; then
  printf 'Erreur : curl est nécessaire pour télécharger les données.\n' >&2
  exit 1
fi

mkdir -p "${OUTPUT_DIR}"

temporary_file="$(mktemp "${OUTPUT_FILE}.tmp.XXXXXX")"
trap 'rm -f "${temporary_file}"' EXIT

printf 'Téléchargement des permis de la Ville de Québec...\n'

curl \
  --fail \
  --location \
  --show-error \
  --retry 3 \
  --connect-timeout 20 \
  --output "${temporary_file}" \
  "${DATASET_URL}"

if [[ ! -s "${temporary_file}" ]]; then
  printf 'Erreur : le fichier téléchargé est vide.\n' >&2
  exit 1
fi

header="$(head -n 1 "${temporary_file}" | tr -d '\r')"
header="${header#$'\xEF\xBB\xBF'}"

if [[ "${header}" != "${EXPECTED_HEADER}" ]]; then
  printf 'Erreur : l’en-tête du fichier CSV est inattendu.\n' >&2
  exit 1
fi

line_count="$(wc -l < "${temporary_file}" | tr -d '[:space:]')"

if (( line_count < 2 )); then
  printf 'Erreur : le fichier CSV ne contient aucune donnée.\n' >&2
  exit 1
fi

mv "${temporary_file}" "${OUTPUT_FILE}"
trap - EXIT

printf 'Téléchargement terminé : %s\n' "${OUTPUT_FILE}"
printf 'Nombre de lignes : %s (%s permis + l’en-tête)\n' \
  "${line_count}" "$((line_count - 1))"
