from django.db import models

class PexesoCard(models.Model):
    czech_word = models.CharField(max_length=100)
    english_word = models.CharField(max_length=100)

    def __str__(self):
        return f"{self.czech_word} - {self.english_word}"