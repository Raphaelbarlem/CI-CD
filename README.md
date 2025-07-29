# Pipeline de CI/CD com Jenkins, ArgoCD e Helm

![Kubernetes](https://img.shields.io/badge/kubernetes-%23326ce5.svg?style=for-the-badge&logo=kubernetes&logoColor=white)
![Jenkins](https://img.shields.io/badge/jenkins-%232C5263.svg?style=for-the-badge&logo=jenkins&logoColor=white)
![Helm](https://img.shields.io/badge/helm-%230F1689.svg?style=for-the-badge&logo=helm&logoColor=white)

Repositório contendo a infraestrutura como código para um pipeline de entrega contínua utilizando:

- **Jenkins** como orquestrador de pipelines
- **ArgoCD** para GitOps e deploy em Kubernetes
- **Helm** para empacotamento de aplicações

## 📦 Componentes Principais

| Ferramenta       | Função                              | Configuração                  |
|------------------|-------------------------------------|-------------------------------|
| Jenkins          | Pipelines de build e testes         | `values/jenkins/values.yaml`  |
| ArgoCD           | Deploy automático via GitOps        | `manifests/argo-application.yaml` |
| MetalLB          | Balanceamento de carga em bare metal| `manifests/metallb-pool.yaml` |
| Harbor           | Registry privado de imagens         | `values/harbor/values.yaml`   |

## 🚀 Como Executar

### Pré-requisitos
- Kubernetes cluster (testado com Kind)
- Helm 3.8+
- kubectl

### Instalação
```bash
make init  # Configura o cluster e namespaces
make deploy-all  # Instala todos os componentes

## 🔧 Estrutura de Arquivos
.
├── manifests/            # Kubernetes manifests para operadores
│   ├── argo-application.yaml  # Config do ArgoCD
│   └── metallb-pool.yaml      # Configuração de IPs
├── values/               # Valores customizados para Helm charts
│   ├── jenkins/          # Configuração do Jenkins
│   └── harbor/           # Configuração do Harbor
└── Makefile              # Automação de tarefas
