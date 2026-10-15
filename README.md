# Практическая работа №6. Небольшая логическая задача с ветвлениями и конечным набором решений

Студент: Ахметгалиев Арслан Альбертович, группа ЭФБО-17-25. Вариант 2.

Программа выбирает маршрут согласования служебной заявки (`SECURITY_REVIEW`, `DIRECTOR_REVIEW`, `FINANCE_REVIEW`,
`FAST_TRACK`, `STANDARD`) по сумме, срочности, уровню риска и наличию персональных данных. По варианту 2 риск `MEDIUM`
наравне с `HIGH` даёт `DIRECTOR_REVIEW`; добавлен метод `addRiskWeight` (LOW = 0, MEDIUM = 50, HIGH = 100).

Java SE 21, без Maven и сторонних библиотек.

## Компиляция

```
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

## Запуск

```
java -Dstdout.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck               # unit-проверки + data/sample.csv
java -Dstdout.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck csv           # только data/sample.csv
java -Dstdout.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck interactive   # amount;urgent;risk;personal, выход: exit или пустая строка
```

Запускать из корня проекта (файл `data/sample.csv` читается по относительному пути).
