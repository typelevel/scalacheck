{-# LANGUAGE OverloadedStrings #-}

module Main where

import Data.Monoid (mempty, mappend, mconcat)
import Data.Maybe (fromJust)
import Data.Yaml (decodeFile)
import Data.Char (isDigit)
import Data.List.Split (splitOn)
import Data.List ((\\), isInfixOf, sortBy)
import qualified Data.Map as M
import Hakyll
import Prelude hiding (id)
import qualified Text.Pandoc as Pandoc
import Data.ByteString.Lazy (ByteString)
import System.Cmd (system)
import System.Environment (getEnv)
import System.FilePath (replaceExtension, takeDirectory)

config :: Configuration
config = defaultConfiguration { providerDirectory = "src" }

cmpVer b a = cmpAll $ zip (splitOn "." a) (splitOn "." b)
  where
    cmpAll [] = EQ
    cmpAll ((x,y):xys) = if cmpOne x y == EQ then cmpAll xys else cmpOne x y
    cmpOne x y | all isDigit (x++y) = compare ((read x)::Int) ((read y)::Int)
               | otherwise = compare x y

defaultPandocCompiler ctx = compile $ pandocCompiler
  >>= applyAsTemplate ctx
  >>= loadAndApplyTemplate "templates/default.html" ctx
  >>= relativizeUrls

baseCtx env = mconcat
  [ listField "versions" (bodyField "version" `mappend` ctx) cmp1
  , listField "oldVersions" (bodyField "oldVersion" `mappend` ctx) cmp2
  , ctx
  ]
  where
    ctx = mconcat
      [ defaultContext
      , M.foldrWithKey (\k v c -> constField k v `mappend` c) mempty env
      ]
    cmp1 = getUnderlying >>= \i -> return $ map (Item i) $ versions env
    cmp2 = getUnderlying >>= \i -> return $ map (Item i) $ oldVersions env

versions env = fromJust $ fmap (sortBy cmpVer . splitOn ",") (M.lookup "allVersions" env)

oldVersions env = versions env \\ [ currentVer ]
  where
    currentVer = fromJust (M.lookup "currentVer" env)

releaseCtx r env = mconcat
  [ listField "scalaVersions" (bodyField "scalaVersion" `mappend` ctx) cmp
  , ctx
  ]
  where
    ctx = mconcat [constField "currentVer" r, baseCtx env]
    cmp = getUnderlying >>= \i -> return $ map (Item i) $ fromJust $ M.lookup r scalaVersions
    scalaVersions = fromJust $ fmap parse (M.lookup "scalaVersions" env)
    parse = M.fromList . map kv . (splitOn ",")
    kv = (\(k,v) -> (k, sortBy cmpVer $ splitOn ":" v)) . split
    split xs = (takeWhile ('=' /=) xs, tail $ dropWhile ('=' /=) xs)

articleCtx env = dateField "date" "%B %e, %Y" `mappend` baseCtx env

mkRelease env r = match "templates/release.markdown" $ version r $ do
  route $ constRoute $ "download/"++r++".html"
  defaultPandocCompiler $ releaseCtx r env

main :: IO ()
main = do
  Just env <- getEnv "HAKYLL_CTX" >>= decodeFile

  hakyllWith config $ do

    match "download.markdown" $ do
      route $ setExtension ".html"
      defaultPandocCompiler $ releaseCtx (fromJust $ M.lookup "currentVer" env) env

    match "*.markdown" $ do
      route $ setExtension ".html"
      defaultPandocCompiler $ baseCtx env

    match "templates/*.html" $ do
      compile $ templateCompiler

    match "css/*" $ do
      route idRoute
      compile $ compressCssCompiler

    match "files/**" $ do
      route idRoute
      compile $ copyFileCompiler

    match "js/*" $ do
      route idRoute
      compile $ copyFileCompiler

    match "fonts/*.ttf" $ do
      route idRoute
      compile $ copyFileCompiler

    match "favicon.ico" $ do
      route idRoute
      compile $ copyFileCompiler

    match "img/*.png" $ do
      route idRoute
      compile $ copyFileCompiler

    match "CNAME" $ do
      route idRoute
      compile $ copyFileCompiler

    sequence_ $ map (mkRelease env) $ versions env

--    match "articles/*" $ do
--      route $ setExtension ".html"
--      compile $ pandocCompiler
--        >>= return . fmap demoteHeaders
--        >>= loadAndApplyTemplate "templates/article.html" (baseCtx env)
--        >>= loadAndApplyTemplate "templates/default.html" (baseCtx env)
--        >>= relativizeUrls
--
--    create ["articles.html"] $ do
--      route idRoute
--      compile $ do
--        articles <- recentFirst =<< loadAll "articles/*"
--        let ctx = constField "title" "Articles" `mappend`
--                  listField "articles" (articleCtx env) (return articles) `mappend`
--                  baseCtx env
--        makeItem ""
--            >>= loadAndApplyTemplate "templates/articles.html" ctx
--            >>= loadAndApplyTemplate "templates/default.html" ctx
--            >>= relativizeUrls
