# Platform detection
is_mingw = is_windows and cc.get_id() == 'gcc'
if is_mingw and ff.get_id() != 'gcc'
  error('If you are using GCC on Windows, you must also use GFortran! Detected ' + ff.get_id())
endif

cython_c_args = ['-DCYTHON_CCOMPLEX=0'] # see gh-18975 for why we need this
if is_mingw
  # For mingw-w64, link statically against the UCRT.
  gcc_link_args = ['-lucrt', '-static']
  add_project_link_arguments(gcc_link_args, language: ['c', 'cpp', 'fortran'])
  # Force gcc to float64 long doubles for compatibility with MSVC
  # builds, for C only.
  add_project_arguments('-mlong-double-64', language: 'c')
  # Make fprintf("%zd") work (see https://github.com/rgommers/scipy/issues/118)
  add_project_arguments('-D__USE_MINGW_ANSI_STDIO=1', language: ['c', 'cpp'])
  # Silence warnings emitted by PyOS_snprintf for (%zd), see
  # https://github.com/rgommers/scipy/issues/118.
  # Use as c_args for extensions containing Cython code
  cython_c_args += ['-Wno-format-extra-args', '-Wno-format']
  # Flag needed to work around BLAS and LAPACK Gfortran dependence on
  # undocumented C feature when passing single character string arguments. See:
  #   https://gcc.gnu.org/bugzilla/show_bug.cgi?id=90329
  #   https://github.com/wch/r-source/blob/838f9d5a7be08f2a8c08e47bcd28756f5d0aac90/src/gnuwin32/MkRules.rules#L121
  add_project_arguments('-fno-optimize-sibling-calls', language: ['fortran'])
endif


pybind11_dep = dependency('pybind11', version: '>=2.10.4')



# Copy the main __init__.py and pxd files to the build dir.
# Needed to trick Cython, it won't do a relative import outside a package
fs = import('fs')
#_cython_tree = declare_dependency(sources: [
_cython_tree = [
  fs.copyfile('__init__.py')
]

cython_args = ['-3', '--fast-fail', '--output-file', '@OUTPUT@', '--include-dir', '@BUILD_ROOT@', '@INPUT@']
cython_cplus_args = ['--cplus'] + cython_args

cython_gen = generator(cython,
  arguments : cython_args,
  output : '@BASENAME@.c',
  depends : _cython_tree)

cython_gen_cpp = generator(cython,
  arguments : cython_cplus_args,
  output : '@BASENAME@.cpp',
  depends : [_cython_tree])

# Check if compiler flags are supported. This is necessary to ensure that SciPy
# can be built with any supported compiler. We need so many warning flags
# because we want to be able to build with `-Werror` in CI; that ensures that
# for new code we add, there are no unexpected new issues introduced.
#
# Cleaning up code so we no longer need some of these warning flags is useful,
# but not a priority.
#
# The standard convention used here is:
#   - for C, drop the leading dash and turn remaining dashes into underscores
#   - for C++, prepend `_cpp` and turn remaining dashes into underscores
#   - for Fortran, prepend `_fflags` and turn remaining dashes into underscores

# C warning flags
Wno_maybe_uninitialized = cc.get_supported_arguments('-Wno-maybe-uninitialized')
Wno_discarded_qualifiers = cc.get_supported_arguments('-Wno-discarded-qualifiers')
Wno_empty_body = cc.get_supported_arguments('-Wno-empty-body')
Wno_implicit_function_declaration = cc.get_supported_arguments('-Wno-implicit-function-declaration')
Wno_parentheses = cc.get_supported_arguments('-Wno-parentheses')
Wno_switch = cc.get_supported_arguments('-Wno-switch')
Wno_unused_label = cc.get_supported_arguments('-Wno-unused-label')
Wno_unused_variable = cc.get_supported_arguments('-Wno-unused-variable')
Wno_incompatible_pointer_types = cc.get_supported_arguments('-Wno-incompatible-pointer-types')

# C++ warning flags
_cpp_Wno_cpp = cpp.get_supported_arguments('-Wno-cpp')
_cpp_Wno_deprecated_declarations = cpp.get_supported_arguments('-Wno-deprecated-declarations')
_cpp_Wno_class_memaccess = cpp.get_supported_arguments('-Wno-class-memaccess')
_cpp_Wno_format_truncation = cpp.get_supported_arguments('-Wno-format-truncation')
_cpp_Wno_non_virtual_dtor = cpp.get_supported_arguments('-Wno-non-virtual-dtor')
_cpp_Wno_sign_compare = cpp.get_supported_arguments('-Wno-sign-compare')
_cpp_Wno_switch = cpp.get_supported_arguments('-Wno-switch')
_cpp_Wno_terminate = cpp.get_supported_arguments('-Wno-terminate')
_cpp_Wno_unused_but_set_variable = cpp.get_supported_arguments('-Wno-unused-but-set-variable')
_cpp_Wno_unused_function = cpp.get_supported_arguments('-Wno-unused-function')
_cpp_Wno_unused_local_typedefs = cpp.get_supported_arguments('-Wno-unused-local-typedefs')
_cpp_Wno_unused_variable = cpp.get_supported_arguments('-Wno-unused-variable')
_cpp_Wno_int_in_bool_context = cpp.get_supported_arguments('-Wno-int-in-bool-context')


# Deal with M_PI & friends; add `use_math_defines` to c_args or cpp_args
# Cython doesn't always get this right itself (see, e.g., gh-16800), so
# explicitly add the define as a compiler flag for Cython-generated code.
if is_windows
  use_math_defines = ['-D_USE_MATH_DEFINES']
else
  use_math_defines = []
endif

# Suppress warning for deprecated Numpy API.
# (Suppress warning messages emitted by #warning directives).
# Replace with numpy_nodepr_api after Cython 3.0 is out
cython_c_args += [_cpp_Wno_cpp, use_math_defines]
cython_cpp_args = cython_c_args

# NumPy include directory - needed in all submodules
# The chdir is needed because within numpy there's an `import signal`
# statement, and we don't want that to pick up scipy's signal module rather
# than the stdlib module. The try-except is needed because when things are
# split across drives on Windows, there is no relative path and an exception
# gets raised. There may be other such cases, so add a catch-all and switch to
# an absolute path. Relative paths are needed when for example a virtualenv is
# placed inside the source tree; Meson rejects absolute paths to places inside
# the source tree.
# For cross-compilation it is often not possible to run the Python interpreter
# in order to retrieve numpy's include directory. It can be specified in the
# cross file instead:
#   [properties]
#   numpy-include-dir = /abspath/to/host-pythons/site-packages/numpy/core/include
#
# This uses the path as is, and avoids running the interpreter.
incdir_numpy = meson.get_external_property('numpy-include-dir', 'not-given')
if incdir_numpy == 'not-given'
  incdir_numpy = run_command(py3,
    [
      '-c',
      '''import os
os.chdir(os.path.join("..", "tools"))
import numpy as np
try:
  incdir = os.path.relpath(np.get_include())
except Exception:
  incdir = np.get_include()
print(incdir)
  '''
    ],
    check: true
  ).stdout().strip()

  # We do need an absolute path to feed to `cc.find_library` below
  _incdir_numpy_abs = run_command(py3,
    ['-c', 'import os; os.chdir(".."); import numpy; print(numpy.get_include())'],
    check: true
  ).stdout().strip()
else
  _incdir_numpy_abs = incdir_numpy
endif
inc_np = include_directories(incdir_numpy)
np_dep = declare_dependency(include_directories: inc_np)

# pyx -> c, pyx -> cpp generators, depending on copied pxd files.
# _qhull.pyx depends also on _lib/messagestream
# spt_cython_gen = generator(cython,
#   arguments : cython_args,
#   output : '@BASENAME@.c',
#   depends : [_cython_tree, _spatial_pxd, _lib_pxd])

ckdtree_src = [
  'ckdtree/src/build.cxx',
  'ckdtree/src/count_neighbors.cxx',
  'ckdtree/src/query.cxx',
  'ckdtree/src/query_ball_point.cxx',
  'ckdtree/src/query_ball_tree.cxx',
  'ckdtree/src/query_pairs.cxx',
  'ckdtree/src/sparse_distances.cxx'
]

ckdtree = py3.extension_module('_ckdtree',
  ckdtree_src + [cython_gen_cpp.process('_ckdtree.pyx')],
  cpp_args: cython_cpp_args,
  include_directories: [
    '../_lib',
    '../_build_utils/src',
    'ckdtree/src'
  ],
  dependencies: np_dep,
  # link_args: version_link_args,
  install: true,
  subdir: 'python/knn_match'
)

py3.install_sources([
    '_ckdtree.pyi'
  ],
  subdir: 'python/knn_match'
)

py3.install_sources([
    '__init__.py',
    '_kdtree.py',
    'ckdtree.py',
    'kdtree.py'
  ],
  subdir: 'python/knn_match'
)
