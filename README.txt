EECS 391 Assignment 2
James Flinn, Anthony Dario
jrf116, ard74

A* is used to calculate the distance between the units. This distance is then used in the utility function, where
the footmen want to be as close as possible to the archers. Initially this proved to be very slow. Caching was
then implemented by using a static hash map to store the distances from a given location. This solution greatly
sped up the algorithm.
