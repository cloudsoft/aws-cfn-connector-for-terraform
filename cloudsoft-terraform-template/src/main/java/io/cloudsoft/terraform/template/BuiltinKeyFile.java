package io.cloudsoft.terraform.template;

import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;

class BuiltinKeyFile extends OpenSSHKeyFile {
    BuiltinKeyFile(String keyName) {
        StringBuilder secretKeyBody = new StringBuilder();
        switch (keyName) {
            case "id_rsa_java":
                secretKeyBody
                        .append("-----BEGIN RSA PRIVATE KEY-----\n")
                        .append("MIIEowIBAAKCAQEAvh9N7+rtaJlW+oqo9F0fRPIqQJ5ZOeQQ6O9u0JdnSA4eYoQr\n")
                        .append("167LXN6ltDJVwbvELNCIFrzxWQVuQJmecPCVQ0cV1GIp7jlXJivafVvZ2ydaasdq\n")
                        .append("vyX4DU50iFMsLHpTcMRsimJmB8GHkhwSX6EXhP4RnChUEigZhaOlRWxcaSXQlG01\n")
                        .append("4CKwczW8pSKiXk573BUVkkP3FDu0JnzksBUIR8oy0GMI9JyeCFVzh2wHZW/VfOcF\n")
                        .append("NtTGgFjFhSOw1dWL/ojsiuZsYdv6GD8fDO2O3vB+DU7RJlD8PgSEDnjnWnrvwKyt\n")
                        .append("v2zlX6Q5qDM22qkYqTtB5dsZw9ppsinh1rYVTwIDAQABAoIBACwDi0Nzm6qPdTdj\n")
                        .append("SmHn/Qk1FtsGzZk9VzzV0U2w/1QLELW164BvEt1ZHftte7TyByRo0liRPT5ip3eF\n")
                        .append("GM0EpUmB92fHofVoiOhpVpeW4ASAQ8pciDXgUGCkPQ4iUyOo3hBJKOeQiBZbk0sb\n")
                        .append("/JTu8kbzmpgQLgZWCjKKBATiZTJiDFAKdqOiuxWm/PnLvnCQiVFkSnBWRbMYpNDY\n")
                        .append("7GfntCgAxSktC/IUnXSbHCi65mp7ME9ZPGbP48zuHaeipaGLyumsOajbkkgD920y\n")
                        .append("g6R3ITq0JzdsPbM9PqwtiO1nK9+LsRLuR2LGiThcjaVWcG8LS0Pj7HaHCVHlJr/x\n")
                        .append("5R8xjqECgYEA7VFQt02lwaYLZtPdyVmLD5XNxOp9B037LZEtUl3ZioRshl6U2k91\n")
                        .append("c1Clecl0IZvxyoJbrRhe3VTcdxrMIQb+B2FIIzL8JCf+V6qL9xFGq4cSHcszirFJ\n")
                        .append("rDhdtHHKZ5LUHEyl0g4I3w/kfSt4X54e3vlkAeWGyexeD48f7aeZgTUCgYEAzRbb\n")
                        .append("UZYtUB6YGgFyVXhUBzSRfhFBmqSZ4znC5UF+Cb+A0VSLNeGvPXW8yUjzqPPrEGcd\n")
                        .append("M0Ri+AEiod/XgbLgN4NIbKhYgyd4RRD/4Zr0nHHlxI9CiXx4dzYbn04eUCbS42ee\n")
                        .append("eaz4pM2X0G7kdDMYdgoGWp6buMb5z2u3usv8sPMCgYB7SsOkd7Kl6J4Dzg5rjRmx\n")
                        .append("/yHoK+rI4Lqd8c6Z/CgIzsOTC5BJ2v4p6rwNiZvL2jjD/PWj+AVQ98WSG1nxzrAr\n")
                        .append("JV9U6igUoLKC2RfeRBtzAblnwSoF5BViY+ZK2NNO+/k2uptrhd8WBCuw2+StyHhB\n")
                        .append("X0+VrKlaPnKforvzvB8EAQKBgAmpDPWf3EyM1F9NOCR8gYDBYiUNDQkKvdDtNJf1\n")
                        .append("6Mjuw1OY5uHH2qhLdnQIwqlvq9/e64oxq3PBIe98CruqQFN9FJlBqMGsx0aBkXv/\n")
                        .append("/4uq9ca3dMvjGA2Nd+meFWFzIrXheJ6EnkWtBXyk7I/opDtTJ493LyCaBsRVb5cV\n")
                        .append("6rYvAoGBAOgv84XjW5x6ZjZxroknSHJ5kpfki0ilV9ABjMdl9dGP0pheoweNrspq\n")
                        .append("8rD6OyjJJpn0sOfW3xSO3FMSQK9vvul0V896FiqAxFbgEgALP9RmNq/D9s0XJIBz\n")
                        .append("GCQGo1mPL1qB5ucYCGoSlm+hLy4GJyw5J4gj9kSmib0jFd9iyz7l\n")
                        .append("-----END RSA PRIVATE KEY-----\n");
                break;
            case "terraform-denis-20191104.pem":
                secretKeyBody
                        .append("-----BEGIN RSA PRIVATE KEY-----\n")
                        .append("MIIEowIBAAKCAQEAq/gFq6Us6FKrnNaUoF5totsHHCFg5fJVLxWLW7YqIXfmUEHJgCPY0ySrVNj5\n")
                        .append("GCumPF3PUpzD7l+dW3dku8irXqHk9U1QfOLwDxDi6kEt5c/6uWSlPvdhmVWtUGjMpL2mmioLSr8l\n")
                        .append("4y2YtuIyycZO/Flk3bs5ouVV+fxhn1VMzejcB0APtSINLlXyR/Jj4BmxymzPf9+ogn6cspo2RyoS\n")
                        .append("50nk8o2Yk+V2DuR6CGbL2bRTnsLGAgqc8lA/O4ABtLSqrQQoWzkMeSmBj2AyzP/LoeifSWQXBrpA\n")
                        .append("u0c5T+c62Ax916AG+VuxaR8MAlQahLWRKgzZjbjMPyXLssE1ksw8TQIDAQABAoIBACEChvQfzLL7\n")
                        .append("y31bo0P1XE46hO7daWA6WCellw6PtR6fiVI2jptORjyheVHcJFSDbHGCAF33QZTsPaRlEq0JI/wS\n")
                        .append("T4TWxnByj2oU77nPsSO//HzB2QZxLgbk71PUChRFY0mMcIZ0kq08/2d7nAwnSNofvsDhQ3sFUq+d\n")
                        .append("xo9zLD8J9rsBqb5S0hofkHBwvtbYUMakH/JXGXB6xzMU8uXNnXpQ2743JhFtXf8nG2VanHWhsMJ1\n")
                        .append("59SuVjbCGJP296fHjYQaDirMm7fXAdfog5an/RKosYqfaxhM3Euvur5P9FXWFBRHdXifQ6g51qio\n")
                        .append("gy0wsl9/0W+kWKA9xvqt5l5KzYECgYEA64vC7pGzmLbXRhaSOxlsqqGUzKoTLsO9xH8wptHpPX4g\n")
                        .append("XopCGNCxnKDJU1KwCGsK++pSgqJLNBjCoW0xEF8yybo/47roOsNjCD3WgRVsUGefzv8Uz3mzopBe\n")
                        .append("Wwa5lRb/13mTclxP/XXpWpam5SRhAdwyfFBX9Mb56dz8rU5T0fUCgYEAuubtceLzsk+TS7wjCxLh\n")
                        .append("8e+FQcmwj2flZjROAdldF/8iE2jOTTO1H2w2H7M7Kaoe1YtwyU3gj7x8x8QPtBDncDe4a0X82HUB\n")
                        .append("8T3KlFlTGahhouIuRyZNGOf/xfzFZx8t0ls1psOIUbY2H2vbM8uJEjCWDSdyF8NA+vphH2vs0fkC\n")
                        .append("gYEA1onZ5sm2tyXyNdhhIIsrus6pZTCTgEixE8R1X7xZoLS8bt+nDEspOuwH82ihBUnaG1F2DZ4c\n")
                        .append("uTrEK5v00GClNa/6eUXIN523Gc6ZDcGhgiqgGD2lisPipfKwvmAFsJHePThy3/rMsKMxBF2rbZIQ\n")
                        .append("+UzCfw/+zOhewG+ThZaOKfECgYBRi8P5tp3BJLsNhi57/Gccmfa4N3mTeC88ooGOiPn3KFgDw+yO\n")
                        .append("/op+BUawGhEsIo+FeGtjSVk7HXAL8N0xIVb5PTZ3h3fEMXkBeoZSp458WV0QJkMuw+VLgp20Eo9G\n")
                        .append("aPIjH6yO8C0gCbIfj316YcmSaCRv5NAA0i4d0vRhM0OGcQKBgFRjfC6neCCUgc+4aqkuORwob4+s\n")
                        .append("BdqzBAY7qfBuAsstbfmPz+ELv29kb4digKwmDZf6uWzqchIZXl4nnB+omj++wvN91d8zn0HcjHz1\n")
                        .append("B+4z6QUBWbVU9G8wUTuYPWmMmSD6H4harfVF4NvkDWyvv05fCkBx9qGBK60dBdl58yPE\n")
                        .append("-----END RSA PRIVATE KEY-----\n");
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown builtin SSH private key name '%s'", keyName));
        }
        init(secretKeyBody.toString(), null);
    }
}
