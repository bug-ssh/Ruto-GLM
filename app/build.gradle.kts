名字: Android CI

于:
  push:
    分支: [ "main" ]
  pull_request:
    分支: [ "main" ]

jobs:
  build:

    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4
    - 名字: set up JDK 11
      uses: actions/setup-java@v4
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: gradle

    - 名字: Grant execute permission for gradlew
      run: chmod +x gradlew
    - 名字: Build with Gradle
      run: ./gradlew build
