# Groovy/Springboot/Vaadin Flow/Gradle Bootstrap

Bootstrap repo roughly following the current/stable versions of:

- Java
- Springboot
- Vaadin Flow
- Gradle
- E2E tests with Cypress

# SETUP ON NEW MACHINE
git clone <PRIVATE_REPO_URL>
cd <REPO_NAME>
git fetch origin
git checkout private
git checkout -b public origin/public
git remote add public https://github.com/dez011/publicworks-vaadin-spring-demo.git
git remote -v

# WORKFLOW (EVERY TIME YOU WORK)
# 1. work on private branch
git checkout private
git add .
git commit -m "msg"
git push origin private

# 2. sync safe changes to public branch
git checkout public
git merge private   # or: git cherry-pick <commit>

# 3. push sanitized public branch to both repos
git push origin public
git push public public:main
