{{- define "member-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "member-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "member-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "member-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "member-service.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{ include "member-service.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.image.tag | default .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "member-service.secretName" -}}
{{- .Values.secrets.existingSecret | default (printf "%s-secrets" (include "member-service.fullname" .)) }}
{{- end }}

{{- /*
member-service.ingress renders one Ingress for ingress.host over TLS. It takes a dict:
  root: the chart's top-level context
  side: the key under .Values.ingress holding entrypoint and annotations ("lan" or "public")
  suffix: appended to the fullname, "" for none
  path: the one Prefix path it serves
The Traefik entrypoint always renders from ingress.<side>.entrypoint; extra annotations can't
override it, so the Ingress never falls onto Traefik's default entrypoints.
*/}}
{{- define "member-service.ingress" -}}
{{- $ingress := .root.Values.ingress }}
{{- $side := index $ingress .side }}
{{- $fullname := include "member-service.fullname" .root }}
{{- $entrypointKey := "traefik.ingress.kubernetes.io/router.entrypoints" }}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ $fullname }}{{ .suffix }}
  labels:
    {{- include "member-service.labels" .root | nindent 4 }}
  annotations:
    {{ $entrypointKey }}: {{ required (printf "ingress.%s.entrypoint is required" .side) $side.entrypoint }}
    {{- with omit ($side.annotations | default dict) $entrypointKey }}
    {{- toYaml . | nindent 4 }}
    {{- end }}
spec:
  {{- with $ingress.className }}
  ingressClassName: {{ . }}
  {{- end }}
  tls:
    - hosts:
        - {{ $ingress.host | quote }}
      secretName: {{ required "ingress.tlsSecretName is required" $ingress.tlsSecretName }}
  rules:
    - host: {{ $ingress.host | quote }}
      http:
        paths:
          - path: {{ .path }}
            pathType: Prefix
            backend:
              service:
                name: {{ $fullname }}
                port:
                  name: http
{{- end }}
