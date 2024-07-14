package info.kgeorgiy.ja.sitkina.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {

    public static final Comparator<Student> COMPARATOR_BY_ID = Comparator.naturalOrder();

    public static final Comparator<Student> COMPARATOR_BY_NAME = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Comparator.comparingInt(Student::getId).reversed());

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortAndGroups(students, COMPARATOR_BY_NAME);

    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortAndGroups(students, COMPARATOR_BY_ID);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestFieldValueByCollector(students, Student::getGroup,
                Collectors.counting(), Comparator.naturalOrder(), Comparator.naturalOrder(), null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return this.<GroupName, Integer>getLargestFieldValueByCollector(students, Student::getGroup,
                Collectors.mapping(Student::getFirstName, Collectors.collectingAndThen(Collectors.toSet(), Set::size)),
                Comparator.naturalOrder(), Comparator.reverseOrder(), null);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getField(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getField(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getField(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getField(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(COMPARATOR_BY_ID)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(COMPARATOR_BY_ID).toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(COMPARATOR_BY_NAME).toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByField(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByField(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByField(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        (name1, name2) -> (name1.compareTo(name2) < 0) ? name1 : name2));
    }

    private <T> List<T> getField(List<Student> students, Function<Student, T> getFieldValue) {
        return students.stream()
                .map(getFieldValue)
                .toList();
    }

    private <T> List<Student> findStudentsByField(Collection<Student> students, T expectedFieldValue,
                                                  Function<Student, T> getFieldValue) {
        return students.stream()
                .filter(student -> getFieldValue.apply(student).equals(expectedFieldValue))
                .sorted(COMPARATOR_BY_NAME)
                .toList();
    }

    private List<Group> getSortAndGroups(Collection<Student> list, Comparator<Student> comparator) {
        return list.stream()
                .sorted(comparator)
                .collect(Collectors.groupingBy(Student::getGroup, Collectors.toList()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map((entry) -> new Group(entry.getKey(), entry.getValue()))
                .toList();
    }

    private <S extends Comparable<? super S>, T extends Comparable<? super T>> S getLargestFieldValueByCollector(
            Collection<Student> students,
            Function<Student, S> getField,
            Collector<Student, ?, T> collector,
            Comparator<T> fieldComparator,
            Comparator<S> groupComparator,
            S defaultValue
    ) {
        return students.stream()
                .collect(Collectors.groupingBy(getField, collector))
                .entrySet()
                .stream()
                .max(Map.Entry.<S, T>comparingByValue(fieldComparator)
                        .thenComparing(Map.Entry.comparingByKey(groupComparator)))
                .map(Map.Entry::getKey)
                .orElse(defaultValue);
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return this.<String, Integer>getLargestFieldValueByCollector(students, Student::getFirstName,
                Collectors.collectingAndThen(Collectors.mapping(Student::getGroup, Collectors.toSet()), Set::size),
                Comparator.naturalOrder(), Comparator.reverseOrder(), "");
    }

    @Override
    public String getLeastPopularName(Collection<Student> students) {
        return this.<String, Integer>getLargestFieldValueByCollector(students, Student::getFirstName,
                Collectors.collectingAndThen(Collectors.mapping(Student::getGroup, Collectors.toSet()), Set::size),
                Comparator.reverseOrder(), Comparator.reverseOrder(), "");
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, StudentDB::getFullName);
    }

    private <T> List<T> getByIndices(List<Student> students, int[] indices, Function<Student, T> getField) {
        return Arrays.stream(indices).mapToObj(i -> getField.apply(students.get(i))).toList();
    }

    private static String getFullName(Student student) {
        return String.format("%s %s", student.getFirstName(), student.getLastName());
    }
}
