# multicast-socket-sd1
Trabalho 1 de Sistemas Distribuídos

# Sobre o trabalho
Arquitetura de Processos Pares, Comunicação em Grupo, Exclusão Mútua e
Segurança.

# Instruções
Siga as instruções abaixo para desenvolver um sistema que garanta a
consistência no acesso a recursos compartilhados (exclusa mútua):
1. Considere um conjunto mínimo inicial de três processos que desejam
acessar recursos compartilhados. Esses processos deverão estar
sincronizados;
2. Considere um conjunto de dois recursos que serão compartilhados;
3. Utilize a comunicação em grupo (multicast) para que os processos se
conheçam e troquem suas chaves públicas. O sistema deve permitir a
entrada e saída de pares durante o funcionamento da aplicação. Todo
evento de entrada e saída de um par deve ser anunciado e a lista de
pares online em cada nó deve ser atualizada a cada evento. Quando um
novo par entrar no grupo multicast, ele deverá enviar sua chave pública
para o grupo (valor 0,5);
4. Utilize a comunicação em grupo (multicast) para que os processos
troquem mensagens para controlar o acesso concorrente aos recursos.
Desenvolva o algoritmo de Ricart e Agrawala (ver Slides da aula –
Coordenação e Acordo em SD). Obs.: ao contrário do algoritmo de
Ricart e Agrawala, caso um processo esteja no estado HELD para um
determinado recurso requisitado, o mesmo deverá retornar uma
resposta negativa e não ficará sem responder (valor 0,5).
5. Defina um Δt1 que será utilizado para aguardar as respostas dos pares.
O não recebimento de uma resposta dentro de Δt1 indicará falha do par.
A lista de pares deverá ser atualizada (valor 0,5).
6. Empregar chaves assimétricas (chave privada e pública) para assinatura
digital do par que está respondendo a um pedido de acesso de modo a
garantir sua autenticidade (valor 0,5).

Obs: utilizar sockets. Pode ser utilizada qualquer linguagem de
programação. É obrigatório documentar todo o código. A aplicação pode ser
desenvolvida individualmente ou em dupla. Porém, a defesa e a nota são
individuais.
