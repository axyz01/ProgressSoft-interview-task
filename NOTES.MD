- Describe in your own words what is z-score and Min-Max scaling and why would we need to use such normalization methods? describe by example, if possible.
```
Data normalization is a technique to organize data in such a way that the data appears similar across 
all fields and records. It helps in removing unnecessary anomalies and ease the analysis of data.
Another goal of normalizing is to logically group data together that are related to each other.

- Z-score is used to measure the number of standard deviations by which the data points in the sample differ 
  from the mean. Alternatively, when defined for population, Z-score can be used to 
  measure the number of standard deviations by which the data points differ from the population mean.
  This is also called as standard score.
  It is denoted using z and calculated as:
  **Z = (x-x̄)/σ**
  where,

  x is an observation in the sample
  x̄ is the mean of the observations in the sample
  σ is the standard deviation of the observations in the sample
  
  
- Min-Max is one of the most common ways to normalize data. For every feature, the minimum value of that feature gets transformed into
  a 0, the maximum value gets transformed into a 1, and every other value gets transformed into a decimal between 0 and 1.
  
  For example, if the minimum value of a feature was 20, and the maximum value was 40, then 30 would be transformed to about 0.5 
  since it is halfway between 20 and 40. The formula is as follows:
  
  (value - min) / (max - min)
  
  
Min-max normalization guarantees all features will have the exact same scale but does not handle outliers well.
But z-score Handles outliers, but does not produce normalized data with the exact same scale.
  
```

- We used Junit5 in our test cases, describe in your own words why would we use such library? 
what is unit testing and why would we use it?
```
I think that the main objective of a library is to deliver faster development of an application.

Unit testing is a powerful tool for software quality, 
It also provides a fundamental check that an application meets its 
software design specifications and behaves as intended.
```
- What is the IDE did you use?
```
IntelliJ IDEA 2021.2.3 (Ultimate Edition)
```

- Name the Java APIs you used?
```
Java SE 8

- java.io.*
- java.math.BigDecimal
- java.math.MathContext;
- java.math.RoundingMode;
- java.nio.file.Files;
- java.nio.file.Path
- java.nio.file.Paths;
- java.util.*
```

- Mention down any difficulties or concerns you faced while working on the task. Your feedback is appreciated :D.
```
All easy! But mathematics wasn't so easy. :D 

I was just wondering why there's a test for thrown expecption messages :D.
I think this was one of the best tasks I've done, and it's well organized as well.

```

- If you implement any of the bonus points, please mentioned it in this file
```
I Provided an executable jar file which allows the evaluator to use the implemented utility through command line.
```

- Did this task enrich technical skills or knowledge? If yes, tell us how: 
```
Yes, it made me more interested in using automation tools like Maven.
```
